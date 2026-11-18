package com.example.unimarket.testutil

import com.example.unimarket.data.local.UserEntity
import com.example.unimarket.data.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Fake untuk [SessionRepository] (asli: DefaultSessionRepository + SharedPreferences).
 *
 * @param loggedInUserId user yang sudah login saat test dimulai; null = belum login.
 * @param userRepository sumber data [currentUser]; pakai FakeUserRepository yang sama
 *                       dengan ViewModel agar perubahan profil ikut terlihat.
 */
class FakeSessionRepository(
    loggedInUserId: Long? = null,
    private val userRepository: FakeUserRepository = FakeUserRepository(),
) : SessionRepository {

    private val _currentUserId = MutableStateFlow(loggedInUserId)
    override val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentUser: Flow<UserEntity?> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(null) else userRepository.observeUser(id)
    }

    /** Berapa kali logout() dipanggil (mis. sesi tidak valid harus di-logout). */
    var logoutCount = 0
        private set

    override fun startSession(userId: Long) {
        _currentUserId.value = userId
    }

    override fun logout() {
        logoutCount++
        _currentUserId.value = null
    }
}
