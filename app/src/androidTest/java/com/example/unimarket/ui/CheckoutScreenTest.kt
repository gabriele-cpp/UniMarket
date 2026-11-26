package com.example.unimarket.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.PurchaseRules
import com.example.unimarket.data.repository.PaymentErrors
import com.example.unimarket.ui.checkout.CheckoutScreen
import com.example.unimarket.ui.checkout.CheckoutTags
import com.example.unimarket.ui.checkout.CheckoutUiState
import com.example.unimarket.ui.theme.UniMarketTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test CheckoutScreen (stateless). Elemen dicari lewat teks yang terlihat;
 * testTag hanya untuk tombol Bayar (teksnya berganti saat memproses) dan indikator loading.
 */
@RunWith(AndroidJUnit4::class)
class CheckoutScreenTest {

    @get:Rule val composeRule = createComposeRule()

    private val kalkulator = Product(
        id = 10,
        title = "Kalkulator Casio fx-991",
        price = 150_000,
        categoryId = 2,
        categoryName = "Elektronik",
        condition = "Baik",
        sellerId = 1L,
        sellerName = "Budi Santoso",
        sellerPhone = "",
        description = "",
    )

    private fun show(state: CheckoutUiState, onPay: () -> Unit = {}, onBack: () -> Unit = {}) =
        composeRule.setContent {
            UniMarketTheme(darkTheme = false) {
                CheckoutScreen(uiState = state, onBack = onBack, onPay = onPay)
            }
        }

    private fun loaded(
        blockReason: String? = null,
        errorMessage: String? = null,
        isProcessing: Boolean = false,
    ) = CheckoutUiState(
        product = kalkulator,
        isLoading = false,
        blockReason = blockReason,
        errorMessage = errorMessage,
        isProcessing = isProcessing,
    )

    @Test
    fun ringkasanBarangHargaDanPenjualTampil() {
        show(loaded())

        composeRule.onNodeWithText("Kalkulator Casio fx-991").assertIsDisplayed()
        composeRule.onNodeWithText("Budi Santoso").assertIsDisplayed()
        composeRule.onNodeWithText("Harga satuan").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(CheckoutTags.PAY_BUTTON).assertIsEnabled()
    }

    @Test
    fun ketukBayar_memanggilOnPay() {
        var clicked = 0
        show(loaded(), onPay = { clicked++ })

        composeRule.onNodeWithText("Bayar").performClick()

        assertEquals(1, clicked)
    }

    @Test
    fun saatMemproses_tombolNonaktifDanLoadingTampil() {
        show(loaded(isProcessing = true))

        composeRule.onNodeWithTag(CheckoutTags.PAY_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithTag(CheckoutTags.PROGRESS, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Bayar").assertDoesNotExist()
    }

    @Test
    fun errorServerPembayaranTampil() {
        show(loaded(errorMessage = PaymentErrors.NETWORK))

        composeRule.onNodeWithText(PaymentErrors.NETWORK).assertIsDisplayed()
        composeRule.onNodeWithTag(CheckoutTags.PAY_BUTTON).assertIsEnabled()   // bisa coba lagi
    }

    @Test
    fun barangMilikSendiri_tombolBayarNonaktifDanAlasanTampil() {
        show(loaded(blockReason = PurchaseRules.OWN_PRODUCT))

        composeRule.onNodeWithText(PurchaseRules.OWN_PRODUCT).assertIsDisplayed()
        composeRule.onNodeWithTag(CheckoutTags.PAY_BUTTON).assertIsNotEnabled()
    }
}
