package com.example.unimarket.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Penyimpanan sesi login. Hanya menyimpan id user yang sedang login;
 * data profil (nama, email, HP) selalu dibaca dari tabel users.
 */
class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("unimarket_prefs", Context.MODE_PRIVATE)

    init {
        // Bersihkan key dari versi lama (sebelum ada tabel users).
        if (prefs.contains(LEGACY_IS_LOGGED_IN)) {
            prefs.edit()
                .remove(LEGACY_IS_LOGGED_IN)
                .remove(LEGACY_USER_EMAIL)
                .remove(LEGACY_USER_NAME)
                .apply()
        }
    }

    /** null jika belum login. */
    fun getLoggedInUserId(): Long? =
        prefs.getLong(KEY_LOGGED_IN_USER_ID, NO_USER).takeIf { it != NO_USER }

    fun setLoggedInUserId(userId: Long) {
        prefs.edit().putLong(KEY_LOGGED_IN_USER_ID, userId).apply()
    }

    fun clearSession() {
        prefs.edit().remove(KEY_LOGGED_IN_USER_ID).apply()
    }

    companion object {
        private const val KEY_LOGGED_IN_USER_ID = "logged_in_user_id"
        private const val NO_USER = -1L

        private const val LEGACY_IS_LOGGED_IN = "is_logged_in"
        private const val LEGACY_USER_EMAIL = "user_email"
        private const val LEGACY_USER_NAME = "user_name"
    }
}
