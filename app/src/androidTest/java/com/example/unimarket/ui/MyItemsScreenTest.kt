package com.example.unimarket.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.ProductStatus
import com.example.unimarket.ui.myitems.MyItemsDialogState
import com.example.unimarket.ui.myitems.MyItemsScreen
import com.example.unimarket.ui.myitems.MyItemsUiState
import com.example.unimarket.ui.theme.UniMarketTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI test MyItemsScreen (stateless): loading, error, kosong, daftar, dan callback aksi. */
@RunWith(AndroidJUnit4::class)
class MyItemsScreenTest {

    @get:Rule val composeRule = createComposeRule()

    private fun product(id: Int, title: String, status: ProductStatus = ProductStatus.TERSEDIA) = Product(
        id = id, title = title, price = 50_000, categoryId = 1, categoryName = "Buku",
        condition = "Baik", sellerId = 1L, sellerName = "Budi", sellerPhone = "6281234567890",
        description = "", status = status,
    )

    private fun show(
        state: MyItemsUiState,
        dialog: MyItemsDialogState = MyItemsDialogState(),
        onBack: () -> Unit = {},
        onProductClick: (Int) -> Unit = {},
        onPostClick: () -> Unit = {},
        onDeleteRequest: (Product) -> Unit = {},
        onDeleteConfirm: () -> Unit = {},
    ) = composeRule.setContent {
        UniMarketTheme(darkTheme = false) {
            MyItemsScreen(
                uiState = state,
                dialogState = dialog,
                onBack = onBack,
                onProductClick = onProductClick,
                onEditClick = {},
                onPostClick = onPostClick,
                onStatusRequest = {},
                onStatusSelected = {},
                onStatusDismiss = {},
                onDeleteRequest = onDeleteRequest,
                onDeleteConfirm = onDeleteConfirm,
                onDeleteDismiss = {},
            )
        }
    }

    @Test
    fun daftarKosong_menampilkanPesanDanTombolJualBarang() {
        var posted = false
        show(MyItemsUiState(isLoading = false), onPostClick = { posted = true })

        composeRule.onNodeWithText("Belum ada barang").assertIsDisplayed()
        composeRule.onNodeWithText("JUAL BARANG").performClick()

        assertTrue(posted)
    }

    @Test
    fun daftarBarang_menampilkanJudulDanRingkasanStatus() {
        show(MyItemsUiState(
            items = listOf(product(1, "Kalkulator Casio"), product(2, "Buku Kalkulus", ProductStatus.TERJUAL)),
            isLoading = false,
        ))

        composeRule.onNodeWithText("Kalkulator Casio").assertIsDisplayed()
        composeRule.onNodeWithText("Buku Kalkulus").assertIsDisplayed()
        composeRule.onNodeWithText("1 aktif · 1 terjual").assertIsDisplayed()
        composeRule.onNodeWithText("Belum ada barang").assertDoesNotExist()
    }

    @Test
    fun pesanErrorTampil() {
        show(MyItemsUiState(isLoading = false, errorMessage = "Gagal memuat data."))

        composeRule.onNodeWithText("Terjadi kesalahan").assertIsDisplayed()
        composeRule.onNodeWithText("Gagal memuat data.").assertIsDisplayed()
    }

    @Test
    fun saatLoading_tidakMenampilkanPesanKosong() {
        show(MyItemsUiState(isLoading = true))

        composeRule.onNodeWithText("Belum ada barang").assertDoesNotExist()
        composeRule.onNodeWithText("JUAL BARANG").assertDoesNotExist()
    }

    @Test
    fun ketukBarang_mengirimIdYangBenar() {
        var clickedId: Int? = null
        show(
            MyItemsUiState(items = listOf(product(7, "Kalkulator Casio")), isLoading = false),
            onProductClick = { clickedId = it },
        )

        composeRule.onNodeWithText("Kalkulator Casio").performClick()

        assertEquals(7, clickedId)
    }

    @Test
    fun ketukHapus_mengirimBarangYangDipilih() {
        val target = product(3, "Novel Bumi")
        var requested: Product? = null
        show(MyItemsUiState(items = listOf(target), isLoading = false), onDeleteRequest = { requested = it })

        composeRule.onNodeWithText("HAPUS").performClick()

        assertEquals(target, requested)
    }

    @Test
    fun dialogKonfirmasiHapus_tombolHapusMemanggilKonfirmasi() {
        val target = product(3, "Novel Bumi")
        var confirmed = false
        show(
            MyItemsUiState(items = listOf(target), isLoading = false),
            dialog = MyItemsDialogState(pendingDelete = target),
            onDeleteConfirm = { confirmed = true },
        )

        composeRule.onNodeWithText("Hapus barang?").assertIsDisplayed()
        composeRule.onNodeWithText("Hapus").performClick()

        assertTrue(confirmed)
    }

    @Test
    fun tombolKembali_memanggilOnBack() {
        var back = false
        show(MyItemsUiState(isLoading = false), onBack = { back = true })

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(back)
    }
}
