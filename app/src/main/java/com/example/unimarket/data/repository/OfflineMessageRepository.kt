package com.example.unimarket.data.repository

import com.example.unimarket.data.local.MessageDao
import com.example.unimarket.data.local.MessageEntity
import com.example.unimarket.data.local.ProductDao
import com.example.unimarket.data.local.UserPreferences
import com.example.unimarket.data.model.Conversation
import com.example.unimarket.data.model.Message
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class OfflineMessageRepository(
    private val messageDao: MessageDao,
    private val productDao: ProductDao,
    private val userPreferences: UserPreferences? = null
) : MessageRepository {

    private val lastReadTimestampFlow = MutableStateFlow(
        userPreferences?.getLastReadChatTimestamp() ?: 0L
    )

    override fun getMessages(productId: Int, buyerId: Long): Flow<List<Message>> =
        messageDao.getMessages(productId, buyerId).map { list -> list.map { it.toMessage() } }

    override fun getConversations(userId: Long): Flow<List<Conversation>> =
        messageDao.getConversations(userId)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeUnreadCount(userId: Long): Flow<Int> =
        lastReadTimestampFlow.flatMapLatest { ts ->
            messageDao.observeUnreadCount(userId, ts)
        }

    override fun markMessagesAsRead() {
        val now = System.currentTimeMillis()
        userPreferences?.setLastReadChatTimestamp(now)
        lastReadTimestampFlow.value = now
    }

    override suspend fun sendMessage(
        productId: Int,
        buyerId: Long,
        senderId: Long,
        text: String
    ): SendMessageResult = when (val result = insertLocalMessage(productId, buyerId, senderId, text)) {
        is LocalInsertResult.Saved -> SendMessageResult.Success
        is LocalInsertResult.Rejected -> SendMessageResult.Error(result.message)
    }

    /** Validasi lalu simpan pesan di Room. Mengembalikan id baris lokal jika berhasil. */
    suspend fun insertLocalMessage(
        productId: Int,
        buyerId: Long,
        senderId: Long,
        text: String
    ): LocalInsertResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return LocalInsertResult.Rejected(ChatErrors.EMPTY)
        if (trimmed.length > ChatErrors.MAX_LENGTH) return LocalInsertResult.Rejected(ChatErrors.TOO_LONG)

        val product = productDao.getProductEntityById(productId)
            ?: return LocalInsertResult.Rejected(ChatErrors.PRODUCT_NOT_FOUND)
        if (buyerId == product.sellerId) return LocalInsertResult.Rejected(ChatErrors.OWN_PRODUCT)
        if (senderId != buyerId && senderId != product.sellerId) {
            return LocalInsertResult.Rejected(ChatErrors.NOT_PARTICIPANT)
        }

        val id = messageDao.insert(
            MessageEntity(productId = productId, buyerId = buyerId, senderId = senderId, text = trimmed)
        )
        return LocalInsertResult.Saved(id, trimmed)
    }
}

sealed class LocalInsertResult {
    data class Saved(val localId: Long, val text: String) : LocalInsertResult()
    data class Rejected(val message: String) : LocalInsertResult()
}
