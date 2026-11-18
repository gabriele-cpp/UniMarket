package com.example.unimarket.ui.login

import com.example.unimarket.data.repository.AuthErrors
import com.example.unimarket.testutil.ErrorLogRule
import com.example.unimarket.testutil.FakeSessionRepository
import com.example.unimarket.testutil.FakeUserRepository
import com.example.unimarket.testutil.MainDispatcherRule
import com.example.unimarket.testutil.user
import com.example.unimarket.ui.util.ErrorMessages
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

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val errorLog = ErrorLogRule()

    private val budi = user(id = 1, name = "Budi", email = "budi@kampus.ac.id")
    private val users = FakeUserRepository(listOf(budi to "rahasia1"))
    private val session = FakeSessionRepository(loggedInUserId = null, userRepository = users)
    private val vm = LoginViewModel(users, session)

    private fun login(email: String, password: String) {
        vm.onEmailChange(email)
        vm.onPasswordChange(password)
        vm.onLogin()
    }

    // ── SUKSES ───────────────────────────────────────────────────────────────
    @Test
    fun `login dengan akun terdaftar memulai sesi`() = runTest {
        login("budi@kampus.ac.id", "rahasia1")
        runCurrent()

        assertTrue(vm.uiState.value.isLoggedIn)
        assertNull(vm.uiState.value.generalError)
        assertEquals(1L, session.currentUserId.value)
    }

    @Test
    fun `email ditulis kapital dan berspasi tetap dikenali`() = runTest {
        login("  Budi@Kampus.ac.id ", "rahasia1")
        runCurrent()

        assertTrue(vm.uiState.value.isLoggedIn)
    }

    // ── GAGAL ────────────────────────────────────────────────────────────────
    @Test
    fun `password salah menampilkan pesan dan tidak login`() = runTest {
        login("budi@kampus.ac.id", "salah123")
        runCurrent()

        assertEquals(AuthErrors.INVALID_CREDENTIALS, vm.uiState.value.generalError)
        assertFalse(vm.uiState.value.isLoggedIn)
        assertNull(session.currentUserId.value)
    }

    @Test
    fun `database bermasalah menampilkan pesan gagal masuk`() = runTest {
        users.error = IllegalStateException("db terkunci")

        login("budi@kampus.ac.id", "rahasia1")
        runCurrent()

        assertEquals(ErrorMessages.LOGIN_FAILED, vm.uiState.value.generalError)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("db terkunci", errorLog.logged.single().message)
    }

    // ── KOSONG / VALIDASI ────────────────────────────────────────────────────
    @Test
    fun `form kosong menampilkan pesan wajib diisi`() = runTest {
        login("", "")
        runCurrent()

        assertEquals("Email wajib diisi", vm.uiState.value.emailError)
        assertEquals("Password wajib diisi", vm.uiState.value.passwordError)
        assertFalse(vm.uiState.value.isLoggedIn)
    }

    @Test
    fun `format email salah ditolak sebelum menghubungi database`() = runTest {
        users.error = IllegalStateException("tidak boleh dipanggil")

        login("budi-kampus", "rahasia1")
        runCurrent()

        assertEquals("Format email tidak valid", vm.uiState.value.emailError)
        assertNull(vm.uiState.value.generalError)       // repository tidak dipanggil
    }

    // ── AKSI PENGGUNA ────────────────────────────────────────────────────────
    @Test
    fun `mengetik ulang menghapus pesan error`() = runTest {
        login("budi@kampus.ac.id", "salah123")
        runCurrent()

        vm.onPasswordChange("rahasia1")

        assertNull(vm.uiState.value.generalError)
        assertNull(vm.uiState.value.passwordError)
    }

    // ── LOADING ──────────────────────────────────────────────────────────────
    @Test
    fun `selama proses login state loading aktif`() = runTest {
        users.delayMillis = 1_000

        login("budi@kampus.ac.id", "rahasia1")
        runCurrent()
        assertTrue(vm.uiState.value.isLoading)

        advanceTimeBy(1_001)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.isLoggedIn)
    }
}
