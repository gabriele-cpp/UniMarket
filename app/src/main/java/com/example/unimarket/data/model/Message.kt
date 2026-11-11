package com.example.unimarket.data.model

data class Message(
    val id: Long,
    val productId: Int,
    val buyerId: Long,
    val senderId: Long,
    val text: String,
    val timestamp: Long
)

/**
 * Ringkasan satu percakapan untuk daftar chat. Diisi langsung oleh query Room
 * (nama properti = nama kolom hasil query di MessageDao.getConversations).
 */
data class Conversation(
    val productId: Int,
    val buyerId: Long,
    val productTitle: String,
    val productImageUri: String?,
    val otherUserName: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val lastSenderId: Long
)
