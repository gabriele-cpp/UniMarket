package com.example.unimarket.ui.checkout

import androidx.lifecycle.SavedStateHandle
import com.example.unimarket.data.model.OrderStatus
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.ProductStatus
import com.example.unimarket.data.model.PurchaseRules
import com.example.unimarket.data.repository.PaymentErrors
import com.example.unimarket.testutil.ErrorLogRule
import com.example.unimarket.testutil.FakePaymentRepository
import com.example.unimarket.testutil.FakeProductRepository
import com.example.unimarket.testutil.FakeSessionRepository
import com.example.unimarket.testutil.MainDispatcherRule
import com.example.unimarket.testutil.product
import com.example.unimarket.ui.util.ErrorMessages
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit test CheckoutViewModel. Jaringan dan Room diganti FakePaymentRepository,
 * sehingga test cepat, tidak butuh payment-server, dan hasilnya selalu sama.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val errorLog = ErrorLogRule()

    private val buyerId = 2L
    private val sellerId = 1L
    private val kalkulator = product(title = "Kalkulator Casio", id = 10, price = 150_000, sellerId = sellerId)

    private fun setup(
        item: Product = kalkulator,
        loggedInUserId: Long? = buyerId,
    ): Triple<CheckoutViewModel, FakePaymentRepository, FakeProductRepository> {
        val products = FakeProductRepository(listOf(item))
        val payments = FakePaymentRepository(products)
        val vm = CheckoutViewModel(
            SavedStateHandle(mapOf(CheckoutViewModel.PRODUCT_ID_ARG to item.id)),
            products,
            FakeSessionRepository(loggedInUserId = loggedInUserId),
            payments,
        )
        return Triple(vm, payments, products)
    }

    // ── MEMUAT ───────────────────────────────────────────────────────────────
    @Test
    fun `membuka checkout menampilkan ringkasan barang dan tombol bayar aktif`() = runTest {
        val (vm, _, _) = setup()
        runCurrent()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Kalkulator Casio", state.product?.title)
        assertEquals(150_000L, state.product?.price)
        assertNull(state.blockReason)
        assertTrue(state.canPay)
    }

    // ── SUKSES ───────────────────────────────────────────────────────────────
    @Test
    fun `bayar membuat order PENDING dan redirectUrl tersedia`() = runTest {
        val (vm, payments, _) = setup()
        runCurrent()

        vm.onPay()
        runCurrent()

        val state = vm.uiState.value
        val order = state.createdOrder
        assertNotNull("order harus dibuat", order)
        assertTrue(order!!.redirectUrl!!.startsWith("https://"))
        assertFalse(state.isProcessing)
        assertNull(state.errorMessage)
        // Order benar-benar tersimpan, untuk barang & pembeli yang benar
        val saved = payments.current.single()
        assertEquals(order.orderId, saved.orderId)
        assertEquals(OrderStatus.PENDING, saved.status)
        assertEquals(150_000L, saved.amount)
        assertEquals(listOf(kalkulator.id to buyerId), payments.createCalls)
    }

    @Test
    fun `selama memproses tombol bayar nonaktif dan ketukan kedua diabaikan`() = runTest {
        val (vm, payments, _) = setup()
        payments.delayMillis = 1_000
        runCurrent()

        vm.onPay()
        runCurrent()
        assertTrue(vm.uiState.value.isProcessing)
        assertFalse(vm.uiState.value.canPay)

        vm.onPay()                       // ketukan ganda
        advanceTimeBy(1_001)

        assertFalse(vm.uiState.value.isProcessing)
        assertEquals(1, payments.createCalls.size)
        assertEquals(1, payments.current.size)
    }

    @Test
    fun `setelah pindah ke layar pembayaran createdOrder dikosongkan`() = runTest {
        val (vm, _, _) = setup()
        runCurrent()
        vm.onPay()
        runCurrent()

        vm.onPaymentOpened()

        assertNull(vm.uiState.value.createdOrder)
    }

    // ── GAGAL ────────────────────────────────────────────────────────────────
    @Test
    fun `server pembayaran tidak bisa dihubungi menampilkan pesan dan bisa dicoba lagi`() = runTest {
        val (vm, payments, _) = setup()
        payments.serverError = PaymentErrors.NETWORK
        runCurrent()

        vm.onPay()
        runCurrent()

        val state = vm.uiState.value
        assertEquals(PaymentErrors.NETWORK, state.errorMessage)
        assertTrue(state.errorMessage!!.startsWith("Gagal terhubung ke server pembayaran"))
        assertNull(state.createdOrder)
        assertFalse(state.isProcessing)
        assertTrue("user bisa menekan Bayar lagi", state.canPay)
        assertTrue(payments.current.isEmpty())
    }

    @Test
    fun `mencoba lagi setelah server pulih berhasil dan pesan error hilang`() = runTest {
        val (vm, payments, _) = setup()
        payments.serverError = PaymentErrors.SERVER
        runCurrent()
        vm.onPay()
        runCurrent()
        assertEquals(PaymentErrors.SERVER, vm.uiState.value.errorMessage)

        payments.serverError = null
        vm.onPay()
        runCurrent()

        assertNull(vm.uiState.value.errorMessage)
        assertNotNull(vm.uiState.value.createdOrder)
    }

    @Test
    fun `exception tak terduga menampilkan pesan umum dan dicatat ke log`() = runTest {
        val (vm, payments, _) = setup()
        val boom = IllegalStateException("database terkunci")
        payments.unexpectedError = boom
        runCurrent()

        vm.onPay()
        runCurrent()

        assertEquals(PaymentErrors.UNKNOWN, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isProcessing)
        assertEquals(listOf<Throwable>(boom), errorLog.logged)
    }

    @Test
    fun `gagal memuat barang menampilkan pesan error`() = runTest {
        val products = FakeProductRepository(listOf(kalkulator)).apply { observeError = IOException("disk") }
        val vm = CheckoutViewModel(
            SavedStateHandle(mapOf(CheckoutViewModel.PRODUCT_ID_ARG to kalkulator.id)),
            products,
            FakeSessionRepository(loggedInUserId = buyerId),
            FakePaymentRepository(products),
        )
        runCurrent()

        assertEquals(ErrorMessages.LOAD_FAILED, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.canPay)
    }

    // ── TIDAK BOLEH DIBELI ───────────────────────────────────────────────────
    @Test
    fun `barang milik sendiri tidak bisa dibeli`() = runTest {
        val (vm, payments, _) = setup(loggedInUserId = sellerId)
        runCurrent()

        vm.onPay()
        runCurrent()

        assertEquals(PurchaseRules.OWN_PRODUCT, vm.uiState.value.blockReason)
        assertFalse(vm.uiState.value.canPay)
        assertTrue("backend tidak boleh dipanggil", payments.createCalls.isEmpty())
        assertNull(vm.uiState.value.createdOrder)
    }

    @Test
    fun `barang yang sudah terjual tidak bisa dibeli`() = runTest {
        val (vm, payments, _) = setup(item = kalkulator.copy(status = ProductStatus.TERJUAL))
        runCurrent()

        vm.onPay()
        runCurrent()

        assertEquals(PurchaseRules.NOT_AVAILABLE, vm.uiState.value.blockReason)
        assertFalse(vm.uiState.value.canPay)
        assertTrue(payments.createCalls.isEmpty())
    }

    @Test
    fun `barang yang sedang negosiasi tidak bisa dibeli`() = runTest {
        val (vm, payments, _) = setup(item = kalkulator.copy(status = ProductStatus.NEGOSIASI))
        runCurrent()

        vm.onPay()

        assertEquals(PurchaseRules.NOT_AVAILABLE, vm.uiState.value.blockReason)
        assertTrue(payments.createCalls.isEmpty())
    }

    @Test
    fun `barang yang tidak ada tidak bisa dibeli`() = runTest {
        val products = FakeProductRepository(emptyList())
        val payments = FakePaymentRepository(products)
        val vm = CheckoutViewModel(
            SavedStateHandle(mapOf(CheckoutViewModel.PRODUCT_ID_ARG to 999)),
            products,
            FakeSessionRepository(loggedInUserId = buyerId),
            payments,
        )
        runCurrent()

        vm.onPay()

        assertNull(vm.uiState.value.product)
        assertEquals(PaymentErrors.PRODUCT_NOT_FOUND, vm.uiState.value.blockReason)
        assertTrue(payments.createCalls.isEmpty())
    }

    @Test
    fun `belum login tidak bisa membayar`() = runTest {
        val (vm, payments, _) = setup(loggedInUserId = null)
        runCurrent()

        vm.onPay()

        assertEquals(ErrorMessages.SESSION_EXPIRED, vm.uiState.value.blockReason)
        assertFalse(vm.uiState.value.canPay)
        assertTrue(payments.createCalls.isEmpty())
    }
}
