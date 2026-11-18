package com.example.unimarket.ui.home

import com.example.unimarket.data.model.ProductInput
import com.example.unimarket.testutil.BUKU
import com.example.unimarket.testutil.ELEKTRONIK
import com.example.unimarket.testutil.ErrorLogRule
import com.example.unimarket.testutil.FakeCategoryRepository
import com.example.unimarket.testutil.FakeProductRepository
import com.example.unimarket.testutil.MainDispatcherRule
import com.example.unimarket.testutil.TEST_CATEGORIES
import com.example.unimarket.testutil.product
import com.example.unimarket.ui.util.ErrorMessages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Unit test HomeViewModel (Pola A: mengamati Flow Room lewat FakeProductRepository). */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val errorLog = ErrorLogRule()

    private val buku = product(title = "Kalkulus Purcell", category = BUKU, createdAt = 2)
    private val laptop = product(title = "Laptop Bekas", category = ELEKTRONIK, createdAt = 3)
    private val novel = product(title = "Novel Laskar Pelangi", category = BUKU, createdAt = 1)

    /** uiState memakai stateIn(WhileSubscribed): perlu "pelanggan" agar mulai mengumpulkan. */
    private fun TestScope.start(vm: HomeViewModel) {
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()
    }

    // ── SUKSES ───────────────────────────────────────────────────────────────
    @Test
    fun `barang dan kategori dari repository tampil dengan barang terbaru di atas`() = runTest {
        val vm = HomeViewModel(FakeProductRepository(listOf(buku, laptop, novel)), FakeCategoryRepository())

        start(vm)

        val state = vm.uiState.value
        assertEquals(listOf("Laptop Bekas", "Kalkulus Purcell", "Novel Laskar Pelangi"), state.products.map { it.title })
        assertEquals(TEST_CATEGORIES, state.categories)
        assertNull(state.selectedCategoryId)
        assertEquals("Semua", state.selectedCategoryName)
        assertNull(state.errorMessage)
    }

    // ── KOSONG ───────────────────────────────────────────────────────────────
    @Test
    fun `belum ada barang menghasilkan daftar kosong tanpa pesan error`() = runTest {
        val vm = HomeViewModel(FakeProductRepository(), FakeCategoryRepository())

        start(vm)

        assertTrue(vm.uiState.value.products.isEmpty())
        assertEquals(TEST_CATEGORIES, vm.uiState.value.categories)   // filter kategori tetap bisa dipakai
        assertNull(vm.uiState.value.errorMessage)
    }

    // ── GAGAL ────────────────────────────────────────────────────────────────
    @Test
    fun `database barang gagal dibaca menampilkan pesan gagal memuat dan mencatat error`() = runTest {
        val repo = FakeProductRepository(listOf(buku)).apply {
            observeError = IllegalStateException("database rusak")
        }
        val vm = HomeViewModel(repo, FakeCategoryRepository())

        start(vm)

        assertEquals(ErrorMessages.LOAD_FAILED, vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.products.isEmpty())
        assertEquals("database rusak", errorLog.logged.single().message)
    }

    @Test
    fun `kategori gagal dibaca juga menampilkan pesan gagal memuat`() = runTest {
        val categories = FakeCategoryRepository().apply { observeError = IllegalStateException("kategori") }
        val vm = HomeViewModel(FakeProductRepository(listOf(buku)), categories)

        start(vm)

        assertEquals(ErrorMessages.LOAD_FAILED, vm.uiState.value.errorMessage)
    }

    // ── AKSI PENGGUNA: filter kategori ───────────────────────────────────────
    @Test
    fun `memilih kategori hanya menampilkan barang di kategori itu`() = runTest {
        val vm = HomeViewModel(FakeProductRepository(listOf(buku, laptop, novel)), FakeCategoryRepository())
        start(vm)

        vm.onCategorySelect(BUKU.id)
        runCurrent()

        val state = vm.uiState.value
        assertEquals(listOf("Kalkulus Purcell", "Novel Laskar Pelangi"), state.products.map { it.title })
        assertEquals(BUKU.id, state.selectedCategoryId)
        assertEquals("Buku", state.selectedCategoryName)
    }

    @Test
    fun `memilih Semua setelah filter menampilkan kembali seluruh barang`() = runTest {
        val vm = HomeViewModel(FakeProductRepository(listOf(buku, laptop, novel)), FakeCategoryRepository())
        start(vm)
        vm.onCategorySelect(ELEKTRONIK.id)
        runCurrent()

        vm.onCategorySelect(null)
        runCurrent()

        assertEquals(3, vm.uiState.value.products.size)
        assertEquals("Semua", vm.uiState.value.selectedCategoryName)
    }

    @Test
    fun `barang baru yang tersimpan di database langsung muncul di beranda`() = runTest {
        val repo = FakeProductRepository(listOf(buku))
        val vm = HomeViewModel(repo, FakeCategoryRepository())
        start(vm)

        repo.addProduct(
            ProductInput(
                title = "Kemeja Flanel", price = 75_000, categoryId = 3, condition = "Baik",
                sellerId = 2L, description = ""
            )
        )
        runCurrent()

        assertEquals("Kemeja Flanel", vm.uiState.value.products.first().title)
        assertEquals(2, vm.uiState.value.products.size)
    }
}
