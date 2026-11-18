package com.example.unimarket.ui.payment

import androidx.lifecycle.SavedStateHandle
import com.example.unimarket.data.model.OrderStatus
import com.example.unimarket.data.repository.PaymentErrors
import com.example.unimarket.data.repository.PaymentResult
import com.example.unimarket.testutil.ErrorLogRule
import com.example.unimarket.testutil.FakePaymentRepository
import com.example.unimarket.testutil.FakeProductRepository
import com.example.unimarket.testutil.MainDispatcherRule
import com.example.unimarket.testutil.product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Unit test PaymentViewModel: fase WebView/hasil dan sinkronisasi status. */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val errorLog = ErrorLogRule()

    private val buyerId = 2L

    /**
     * Membuat satu order PENDING di fake, lalu ViewModel untuk order tersebut.
     * MainDispatcherRule memakai UnconfinedTestDispatcher: init ViewModel langsung berjalan,
     * jadi [midtransStatus] harus diatur SEBELUM ViewModel dibuat.
     */
    private suspend fun setup(
        openWebView: Boolean,
        midtransStatus: String = "pending",
    ): Pair<PaymentViewModel, FakePaymentRepository> {
        val products = FakeProductRepository(listOf(product(title = "Kalkulator", id = 10, sellerId = 1L)))
        val payments = FakePaymentRepository(products)
        val order = (payments.createOrder(10, buyerId) as PaymentResult.Success).data
        payments.midtransStatus = midtransStatus
        val vm = PaymentViewModel(
            SavedStateHandle(
                mapOf(
                    PaymentViewModel.ORDER_ID_ARG to order.orderId,
                    PaymentViewModel.OPEN_WEB_VIEW_ARG to openWebView,
                )
            ),
            payments,
        )
        return vm to payments
    }

    @Test
    fun `dari checkout halaman Midtrans langsung terbuka`() = runTest {
        val (vm, _) = setup(openWebView = true)
        runCurrent()

        assertEquals(PaymentPhase.WEBVIEW, vm.uiState.value.phase)
        assertEquals(OrderStatus.PENDING, vm.uiState.value.order?.status)
    }

    @Test
    fun `url finish menutup WebView dan status lunas tampil`() = runTest {
        val (vm, payments) = setup(openWebView = true)
        runCurrent()
        payments.midtransStatus = "settlement"

        vm.onLeavePaymentPage()
        runCurrent()

        assertEquals(PaymentPhase.RESULT, vm.uiState.value.phase)
        assertEquals(OrderStatus.PAID, vm.uiState.value.order?.status)
        assertFalse(vm.uiState.value.isChecking)
    }

    @Test
    fun `dari riwayat status dicek otomatis tanpa membuka WebView`() = runTest {
        val (vm, _) = setup(openWebView = false, midtransStatus = "expire")
        runCurrent()

        assertEquals(PaymentPhase.RESULT, vm.uiState.value.phase)
        assertEquals(OrderStatus.KEDALUWARSA, vm.uiState.value.order?.status)
    }

    @Test
    fun `cek status saat server mati menampilkan pesan dan status tidak berubah`() = runTest {
        val (vm, payments) = setup(openWebView = true)
        runCurrent()
        payments.serverError = PaymentErrors.NETWORK

        vm.checkStatus()
        runCurrent()

        assertEquals(PaymentErrors.NETWORK, vm.uiState.value.message)
        assertEquals(OrderStatus.PENDING, vm.uiState.value.order?.status)

        vm.onMessageShown()
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun `pesanan pending bisa melanjutkan pembayaran`() = runTest {
        val (vm, _) = setup(openWebView = false)
        runCurrent()
        assertEquals(PaymentPhase.RESULT, vm.uiState.value.phase)

        vm.onContinuePayment()

        assertEquals(PaymentPhase.WEBVIEW, vm.uiState.value.phase)
    }
}
