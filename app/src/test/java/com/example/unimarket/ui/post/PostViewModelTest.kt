package com.example.unimarket.ui.post

import androidx.lifecycle.SavedStateHandle
import com.example.unimarket.testutil.BUKU
import com.example.unimarket.testutil.ELEKTRONIK
import com.example.unimarket.testutil.ErrorLogRule
import com.example.unimarket.testutil.FakeCategoryRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit test PostViewModel (form pasang/edit iklan).
 * uiState berupa MutableStateFlow biasa, jadi tidak perlu "pelanggan" seperti Home/Search.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PostViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val errorLog = ErrorLogRule()

    private val sellerId = 1L

    private fun viewModel(
        products: FakeProductRepository = FakeProductRepository(),
        categories: FakeCategoryRepository = FakeCategoryRepository(),
        session: FakeSessionRepository = FakeSessionRepository(loggedInUserId = sellerId),
        editProductId: Int? = null,
    ) = PostViewModel(
        SavedStateHandle(editProductId?.let { mapOf(PostViewModel.PRODUCT_ID_ARG to it) } ?: emptyMap()),
        products, categories, session,
    )

    private fun PostViewModel.fillValidForm() {
        onTitleChange("Kalkulator Casio fx-991")
        onPriceChange("150000")
        onCategoryChange(ELEKTRONIK.id)
    }

    // ── SUKSES ───────────────────────────────────────────────────────────────
    @Test
    fun `pasang iklan dengan data lengkap menyimpan barang dan menampilkan sukses`() = runTest {
        val repo = FakeProductRepository()
        val vm = viewModel(products = repo)
        vm.fillValidForm()

        vm.onPublish()
        runCurrent()

        assertTrue(vm.uiState.value.showSuccess)
        assertFalse(vm.uiState.value.isSaving)
        val saved = repo.current.single()
        assertEquals("Kalkulator Casio fx-991", saved.title)
        assertEquals(150_000L, saved.price)
        assertEquals(ELEKTRONIK.id, saved.categoryId)
        assertEquals(sellerId, saved.sellerId)
        assertEquals("COD Kampus & GoSend/GrabExpress", saved.deliveryMethod)
        assertEquals("Area Kampus", saved.location)          // lokasi kosong → default
    }

    @Test
    fun `kategori pertama otomatis terpilih saat form dibuka`() = runTest {
        val vm = viewModel()
        runCurrent()

        assertEquals(BUKU.id, vm.uiState.value.categoryId)
        assertEquals(3, vm.uiState.value.categories.size)
    }

    // ── VALIDASI ─────────────────────────────────────────────────────────────
    @Test
    fun `judul kosong menampilkan pesan dan tidak menyimpan`() = runTest {
        val repo = FakeProductRepository()
        val vm = viewModel(products = repo)
        vm.fillValidForm()
        vm.onTitleChange("   ")

        vm.onPublish()
        runCurrent()

        assertEquals("Judul barang wajib diisi", vm.uiState.value.titleError)
        assertTrue(repo.current.isEmpty())
        assertFalse(vm.uiState.value.showSuccess)
    }

    @Test
    fun `harga nol ditolak`() = runTest {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onPriceChange("0")

        vm.onPublish()

        assertEquals("Harga harus angka antara 1 dan Rp1.000.000.000", vm.uiState.value.priceError)
    }

    @Test
    fun `tanpa opsi pengiriman tidak dapat dipublikasikan`() = runTest {
        val repo = FakeProductRepository()
        val vm = viewModel(products = repo)
        vm.fillValidForm()
        vm.onToggleCod(false)
        vm.onToggleGoSend(false)

        vm.onPublish()
        runCurrent()

        assertEquals("Pilih minimal 1 opsi pengiriman", vm.uiState.value.deliveryError)
        assertTrue(repo.current.isEmpty())
    }

    @Test
    fun `input harga hanya menyimpan digit`() = runTest {
        val vm = viewModel()

        vm.onPriceChange("Rp 12.500abc")

        assertEquals("12500", vm.uiState.value.price)
    }

    // ── KOSONG ───────────────────────────────────────────────────────────────
    @Test
    fun `tanpa kategori form menolak publish`() = runTest {
        val repo = FakeProductRepository()
        val vm = viewModel(products = repo, categories = FakeCategoryRepository(emptyList()))
        runCurrent()
        vm.onTitleChange("Kalkulator")
        vm.onPriceChange("150000")

        vm.onPublish()
        runCurrent()

        assertNull(vm.uiState.value.categoryId)
        assertEquals("Pilih kategori barang", vm.uiState.value.categoryError)
        assertTrue(repo.current.isEmpty())
    }

    // ── GAGAL ────────────────────────────────────────────────────────────────
    @Test
    fun `penyimpanan gagal menampilkan pesan dan form tetap terisi`() = runTest {
        val repo = FakeProductRepository().apply { writeError = IllegalStateException("disk penuh") }
        val vm = viewModel(products = repo)
        vm.fillValidForm()

        vm.onPublish()
        runCurrent()

        assertEquals(ErrorMessages.SAVE_FAILED, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isSaving)
        assertFalse(vm.uiState.value.showSuccess)
        assertEquals("Kalkulator Casio fx-991", vm.uiState.value.title)   // user tidak perlu mengetik ulang
        assertEquals(1, errorLog.logged.size)
    }

    @Test
    fun `foto gagal disalin menampilkan pesan foto`() = runTest {
        val repo = FakeProductRepository().apply { writeError = IOException("foto rusak") }
        val vm = viewModel(products = repo)
        vm.fillValidForm()

        vm.onPublish()
        runCurrent()

        assertEquals(ErrorMessages.IMAGE_FAILED, vm.uiState.value.errorMessage)
    }

    @Test
    fun `belum login tidak dapat memasang iklan`() = runTest {
        val repo = FakeProductRepository()
        val vm = viewModel(products = repo, session = FakeSessionRepository(loggedInUserId = null))
        vm.fillValidForm()

        vm.onPublish()
        runCurrent()

        assertEquals(ErrorMessages.SESSION_EXPIRED, vm.uiState.value.errorMessage)
        assertTrue(repo.current.isEmpty())
    }

    // ── MODE EDIT ────────────────────────────────────────────────────────────
    @Test
    fun `mode edit memuat data barang lama`() = runTest {
        val lama = product(id = 5, title = "Buku Kalkulus", price = 90_000, category = BUKU, sellerId = sellerId)
        val vm = viewModel(products = FakeProductRepository(listOf(lama)), editProductId = 5)
        runCurrent()

        val state = vm.uiState.value
        assertTrue(state.isEditMode)
        assertFalse(state.isLoadingProduct)
        assertEquals("Buku Kalkulus", state.title)
        assertEquals("90000", state.price)
        assertEquals(BUKU.id, state.categoryId)
        assertTrue(state.allowCod)          // deliveryMethod default "COD Kampus"
        assertFalse(state.allowGoSend)
    }

    @Test
    fun `mengedit barang orang lain ditolak`() = runTest {
        val milikOrangLain = product(id = 7, title = "Laptop", sellerId = 2L)
        val vm = viewModel(products = FakeProductRepository(listOf(milikOrangLain)), editProductId = 7)
        runCurrent()

        assertTrue(vm.uiState.value.loadFailed)
        assertEquals("Anda hanya bisa mengubah barang milik sendiri", vm.uiState.value.errorMessage)
        assertEquals("", vm.uiState.value.title)        // data orang lain tidak dimuat
    }

    // ── LOADING ──────────────────────────────────────────────────────────────
    @Test
    fun `selama menyimpan state saving aktif dan publish ganda diabaikan`() = runTest {
        val repo = FakeProductRepository().apply { writeDelayMillis = 1_000 }
        val vm = viewModel(products = repo)
        vm.fillValidForm()

        vm.onPublish()
        runCurrent()
        assertTrue(vm.uiState.value.isSaving)
        vm.onPublish()                                  // diketuk dua kali

        advanceTimeBy(1_001)

        assertFalse(vm.uiState.value.isSaving)
        assertTrue(vm.uiState.value.showSuccess)
        assertEquals(1, repo.current.size)              // tidak tersimpan dua kali
    }
}
