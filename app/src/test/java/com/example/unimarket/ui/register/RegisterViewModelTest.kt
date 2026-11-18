package com.example.unimarket.ui.register

import com.example.unimarket.data.repository.AuthErrors
import com.example.unimarket.testutil.ErrorLogRule
import com.example.unimarket.testutil.FakeSessionRepository
import com.example.unimarket.testutil.FakeUserRepository
import com.example.unimarket.testutil.MainDispatcherRule
import com.example.unimarket.testutil.user
import com.example.unimarket.ui.util.ErrorMessages
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()
    @get:Rule val errorLog = ErrorLogRule()

    private val users = FakeUserRepository(listOf(user(id = 1, email = "ani@kampus.ac.id") to "rahasia1"))
    private val session = FakeSessionRepository(loggedInUserId = null, userRepository = users)
    private val vm = RegisterViewModel(users, session)

    private fun fill(
        name: String = "Budi Santoso",
        email: String = "budi@kampus.ac.id",
        phone: String = "081234567890",
        password: String = "rahasia1",
        confirm: String = password,
    ) {
        vm.onNameChange(name)
        vm.onEmailChange(email)
        vm.onPhoneChange(phone)
        vm.onPasswordChange(password)
        vm.onConfirmPasswordChange(confirm)
    }

    // ── SUKSES ───────────────────────────────────────────────────────────────
    @Test
    fun `daftar dengan data valid langsung login`() = runTest {
        fill()

        vm.onRegister()
        runCurrent()

        assertTrue(vm.uiState.value.isRegistered)
        val baru = users.current.single { it.email == "budi@kampus.ac.id" }
        assertEquals("Budi Santoso", baru.name)
        assertEquals("6281234567890", baru.phone)          // disimpan dalam format 62
        assertEquals(baru.id, session.currentUserId.value)
    }

    // ── GAGAL ────────────────────────────────────────────────────────────────
    @Test
    fun `email yang sudah dipakai ditandai di kolom email`() = runTest {
        fill(email = "ani@kampus.ac.id")

        vm.onRegister()
        runCurrent()

        assertEquals(AuthErrors.EMAIL_TAKEN, vm.uiState.value.emailError)
        assertNull(vm.uiState.value.generalError)
        assertFalse(vm.uiState.value.isRegistered)
        assertEquals(1, users.current.size)
    }

    @Test
    fun `database bermasalah menampilkan pesan gagal mendaftar`() = runTest {
        users.error = IllegalStateException("db penuh")
        fill()

        vm.onRegister()
        runCurrent()

        assertEquals(ErrorMessages.REGISTER_FAILED, vm.uiState.value.generalError)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(session.currentUserId.value)
        assertEquals(1, errorLog.logged.size)
    }

    // ── VALIDASI ─────────────────────────────────────────────────────────────
    @Test
    fun `konfirmasi password berbeda tidak mendaftar`() = runTest {
        fill(confirm = "rahasia2")

        vm.onRegister()
        runCurrent()

        assertEquals("Konfirmasi password tidak sama", vm.uiState.value.confirmPasswordError)
        assertEquals(1, users.current.size)
        assertFalse(vm.uiState.value.isRegistered)
    }

    @Test
    fun `password kurang dari 6 karakter ditolak`() = runTest {
        fill(password = "abc", confirm = "abc")

        vm.onRegister()

        assertEquals("Password minimal 6 karakter", vm.uiState.value.passwordError)
    }

    // ── KOSONG ───────────────────────────────────────────────────────────────
    @Test
    fun `form kosong menampilkan semua pesan wajib diisi`() = runTest {
        vm.onRegister()
        runCurrent()

        val s = vm.uiState.value
        assertEquals("Nama wajib diisi", s.nameError)
        assertEquals("Email wajib diisi", s.emailError)
        assertEquals("Nomor HP wajib diisi", s.phoneError)
        assertEquals("Password wajib diisi", s.passwordError)
        assertEquals("Konfirmasi password wajib diisi", s.confirmPasswordError)
        assertEquals(1, users.current.size)
    }

    // ── AKSI PENGGUNA ────────────────────────────────────────────────────────
    @Test
    fun `nomor HP hanya menyimpan digit`() = runTest {
        vm.onPhoneChange("0812-3456-7890")

        assertEquals("081234567890", vm.uiState.value.phone)
    }

    @Test
    fun `mengubah email menghapus pesan email sudah terdaftar`() = runTest {
        fill(email = "ani@kampus.ac.id")
        vm.onRegister()
        runCurrent()
        assertNotNull(vm.uiState.value.emailError)

        vm.onEmailChange("budi@kampus.ac.id")

        assertNull(vm.uiState.value.emailError)
    }
}
