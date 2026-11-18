package com.example.unimarket.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.unimarket.ui.login.LoginScreen
import com.example.unimarket.ui.login.LoginUiState
import com.example.unimarket.ui.theme.UniMarketTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test LoginScreen (stateless). Elemen dicari seperti pengguna mencarinya:
 * lewat teks yang terlihat, bukan testTag.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule val composeRule = createComposeRule()

    private fun show(
        state: LoginUiState = LoginUiState(),
        onEmailChange: (String) -> Unit = {},
        onPasswordChange: (String) -> Unit = {},
        onLogin: () -> Unit = {},
        onRegisterClick: () -> Unit = {},
    ) = composeRule.setContent {
        UniMarketTheme(darkTheme = false) {
            LoginScreen(
                uiState = state,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onLogin = onLogin,
                onRegisterClick = onRegisterClick,
            )
        }
    }

    @Test
    fun pesanErrorValidasiDanLoginTampilDiLayar() {
        show(LoginUiState(emailError = "Format email tidak valid", generalError = "Email atau password salah"))

        composeRule.onNodeWithText("Format email tidak valid").assertIsDisplayed()
        composeRule.onNodeWithText("Email atau password salah").assertIsDisplayed()
    }

    @Test
    fun ketukMasuk_memanggilOnLogin() {
        var clicked = 0
        show(onLogin = { clicked++ })

        composeRule.onNodeWithText("MASUK").performClick()

        assertEquals(1, clicked)
    }

    @Test
    fun saatLoading_teksMasukDigantiIndikatorLoading() {
        show(LoginUiState(isLoading = true))

        composeRule.onNodeWithText("MASUK").assertDoesNotExist()
        composeRule.onNodeWithText("Belum punya akun? Daftar").assertIsDisplayed()   // layar tetap tampil
    }

    @Test
    fun mengetikEmail_mengirimTeksKeCallback() {
        var email = ""
        show(onEmailChange = { email = it })

        composeRule.onNodeWithText("Email Kampus").performTextInput("budi@kampus.ac.id")

        assertEquals("budi@kampus.ac.id", email)
    }

    @Test
    fun ketukDaftar_membukaHalamanRegister() {
        var opened = false
        show(onRegisterClick = { opened = true })

        composeRule.onNodeWithText("Belum punya akun? Daftar").performClick()

        assertTrue(opened)
    }
}
