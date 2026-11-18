package com.example.unimarket.ui.search

import com.example.unimarket.data.model.ProductSort
import com.example.unimarket.data.model.ProductStatus
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit test SearchViewModel: kata kunci di-debounce 300 ms (diuji dengan jam virtual
 * advanceTimeBy, tanpa menunggu sungguhan), filter kategori, urutan, dan error.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val errorLog = ErrorLogRule()

    private val kalkulator = product(title = "Kalkulator Casio", price = 150_000, category = ELEKTRONIK, createdAt = 3)
    private val kalkulus = product(title = "Buku Kalkulus", price = 90_000, category = BUKU, createdAt = 2)
    private val novel = product(title = "Novel Bumi", price = 40_000, category = BUKU, createdAt = 1)
    private val all = listOf(kalkulator, kalkulus, novel)

    private val debounce = SearchViewModel.SEARCH_DEBOUNCE_MS

    private fun TestScope.start(vm: SearchViewModel) {
        backgroundScope.launch { vm.uiState.collect { } }
        runCurrent()
    }

    private fun SearchViewModel.titles() = uiState.value.results.map { it.title }

    // ── SUKSES ───────────────────────────────────────────────────────────────
    @Test
    fun `tanpa kata kunci semua barang tersedia tampil dengan kategori`() = runTest {
        val vm = SearchViewModel(FakeProductRepository(all), FakeCategoryRepository())

        start(vm)

        assertEquals(listOf("Kalkulator Casio", "Buku Kalkulus", "Novel Bumi"), vm.titles())
        assertEquals(TEST_CATEGORIES, vm.uiState.value.categories)
        assertEquals(ProductSort.NEWEST, vm.uiState.value.sort)
        assertNull(vm.uiState.value.errorMessage)
    }

    // ── WAKTU VIRTUAL: debounce ──────────────────────────────────────────────
    @Test
    fun `kata kunci baru diterapkan setelah debounce 300 ms`() = runTest {
        val vm = SearchViewModel(FakeProductRepository(all), FakeCategoryRepository())
        start(vm)

        vm.onQueryChange("kalku")
        assertEquals("kalku", vm.query.value)          // TextField langsung terisi (tanpa debounce)

        advanceTimeBy(debounce - 1)                    // 299 ms: belum dicari
        assertEquals(3, vm.uiState.value.results.size)

        advanceTimeBy(2)                               // 301 ms
        assertEquals(listOf("Kalkulator Casio", "Buku Kalkulus"), vm.titles())
    }

    @Test
    fun `mengetik cepat hanya mencari kata kunci terakhir`() = runTest {
        val vm = SearchViewModel(FakeProductRepository(all), FakeCategoryRepository())
        start(vm)

        vm.onQueryChange("kalku")
        advanceTimeBy(debounce - 100)                  // belum 300 ms, user mengetik lagi
        vm.onQueryChange("novel")
        advanceTimeBy(debounce - 1)                    // 299 ms sejak ketikan terakhir
        assertEquals(3, vm.uiState.value.results.size) // "kalku" tidak pernah diterapkan

        advanceTimeBy(2)
        assertEquals(listOf("Novel Bumi"), vm.titles())
    }

    @Test
    fun `mengosongkan kata kunci langsung menampilkan semua barang tanpa debounce`() = runTest {
        val vm = SearchViewModel(FakeProductRepository(all), FakeCategoryRepository())
        start(vm)
        vm.onQueryChange("novel")
        advanceTimeBy(debounce + 1)
        assertEquals(1, vm.uiState.value.results.size)

        vm.onQueryChange("")
        runCurrent()                                   // tidak memajukan waktu sama sekali

        assertEquals(3, vm.uiState.value.results.size)
    }

    // ── KOSONG ───────────────────────────────────────────────────────────────
    @Test
    fun `kata kunci tanpa hasil menghasilkan daftar kosong tanpa error`() = runTest {
        val vm = SearchViewModel(FakeProductRepository(all), FakeCategoryRepository())
        start(vm)

        vm.onQueryChange("sepeda")
        advanceTimeBy(debounce + 1)

        assertTrue(vm.uiState.value.results.isEmpty())
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `barang terjual tidak muncul di hasil pencarian`() = runTest {
        val terjual = product(title = "Kalkulator Bekas", status = ProductStatus.TERJUAL)
        val vm = SearchViewModel(FakeProductRepository(all + terjual), FakeCategoryRepository())
        start(vm)

        vm.onQueryChange("kalkulator")
        advanceTimeBy(debounce + 1)

        assertEquals(listOf("Kalkulator Casio"), vm.titles())
    }

    // ── GAGAL ────────────────────────────────────────────────────────────────
    @Test
    fun `pencarian gagal menampilkan pesan error tetapi kategori tetap tampil`() = runTest {
        val repo = FakeProductRepository(all).apply { observeError = IllegalStateException("query gagal") }
        val vm = SearchViewModel(repo, FakeCategoryRepository())

        start(vm)

        assertEquals(ErrorMessages.LOAD_FAILED, vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.results.isEmpty())
        assertEquals(TEST_CATEGORIES, vm.uiState.value.categories)   // layar tidak kosong total
        assertEquals(1, errorLog.logged.size)
    }

    // ── AKSI PENGGUNA: kategori & urutan ─────────────────────────────────────
    @Test
    fun `filter kategori digabung dengan kata kunci`() = runTest {
        val vm = SearchViewModel(FakeProductRepository(all), FakeCategoryRepository())
        start(vm)

        vm.onCategoryChange(BUKU.id)
        vm.onQueryChange("kalku")
        advanceTimeBy(debounce + 1)

        assertEquals(listOf("Buku Kalkulus"), vm.titles())
        assertEquals("Buku", vm.uiState.value.selectedCategoryName)
    }

    @Test
    fun `urutan harga termurah mengurutkan hasil dari harga terendah`() = runTest {
        val vm = SearchViewModel(FakeProductRepository(all), FakeCategoryRepository())
        start(vm)

        vm.onSortChange(ProductSort.PRICE_ASC)
        runCurrent()

        assertEquals(listOf(40_000L, 90_000L, 150_000L), vm.uiState.value.results.map { it.price })
        assertEquals(ProductSort.PRICE_ASC, vm.uiState.value.sort)
    }
}
