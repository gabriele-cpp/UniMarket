package com.example.unimarket.data.repository

import com.example.unimarket.data.model.Conversation
import com.example.unimarket.data.model.Message
import kotlinx.coroutines.flow.Flow

/**
 * Chat antara pembeli dan penjual (simulasi lokal di satu perangkat).
 * Satu percakapan = (productId, buyerId).
 */
interface MessageRepository {
    fun getMessages(productId: Int, buyerId: Long): Flow<List<Message>>

    fun getConversations(userId: Long): Flow<List<Conversation>>

    /**
     * Mengirim pesan. Hanya pembeli percakapan atau penjual barang yang boleh mengirim,
     * dan penjual tidak bisa menjadi "pembeli" untuk barangnya sendiri.
     */
    suspend fun sendMessage(productId: Int, buyerId: Long, senderId: Long, text: String): SendMessageResult
}

sealed class SendMessageResult {
    data object Success : SendMessageResult()
    data class Error(val message: String) : SendMessageResult()
}

object ChatErrors {
    const val EMPTY = "Pesan tidak boleh kosong"
    const val TOO_LONG = "Pesan terlalu panjang (maksimal 1000 karakter)"
    const val PRODUCT_NOT_FOUND = "Barang tidak ditemukan"
    const val OWN_PRODUCT = "Anda tidak bisa chat untuk barang milik sendiri"
    const val NOT_PARTICIPANT = "Anda bukan bagian dari percakapan ini"
    const val MAX_LENGTH = 1000
}
