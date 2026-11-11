package com.example.unimarket.data.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Hash password sederhana (SHA-256 + salt acak) untuk penyimpanan lokal.
 * Format hasil: "<saltBase64>:<hashBase64>". Dipakai juga nanti di Langkah 1B (login/registrasi).
 */
object PasswordHasher {
    private const val SALT_BYTES = 16

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(salt) + ":" + encoder.encodeToString(digest(salt, password))
    }

    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val decoder = Base64.getDecoder()
        val salt = runCatching { decoder.decode(parts[0]) }.getOrNull() ?: return false
        val expected = runCatching { decoder.decode(parts[1]) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, digest(salt, password))
    }

    private fun digest(salt: ByteArray, password: String): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(password.toByteArray(Charsets.UTF_8))
    }
}
