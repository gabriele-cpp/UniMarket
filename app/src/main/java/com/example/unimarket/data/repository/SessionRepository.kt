package com.example.unimarket.data.repository

import com.example.unimarket.data.local.UserEntity
import com.example.unimarket.data.local.UserPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Sesi login aktif. Menjadi satu-satunya sumber "siapa user yang sedang login".
 */
interface SessionRepository {
    /** null = belum login. */
    val currentUserId: StateFlow<Long?>

    /** Data user yang sedang login, ikut ter-update jika profil diubah. null = belum login. */
    val currentUser: Flow<UserEntity?>

    fun startSession(userId: Long)

    fun logout()
}

class DefaultSessionRepository(
    private val userPreferences: UserPreferences,
    private val userRepository: UserRepository
) : SessionRepository {

    private val _currentUserId = MutableStateFlow(userPreferences.getLoggedInUserId())
    override val currentUserId: StateFlow<Long?> = _currentUserId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    override val currentUser: Flow<UserEntity?> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(null) else userRepository.observeUser(id)
    }

    override fun startSession(userId: Long) {
        userPreferences.setLoggedInUserId(userId)
        _currentUserId.value = userId
    }

    override fun logout() {
        userPreferences.clearSession()
        _currentUserId.value = null
    }
}
