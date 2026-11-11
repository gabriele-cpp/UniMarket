package com.example.unimarket.data.repository

import com.example.unimarket.data.local.MessageDao
import com.example.unimarket.data.local.MessageEntity
import com.example.unimarket.data.local.ProductDao
import com.example.unimarket.data.model.Conversation
import com.example.unimarket.data.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineMessageRepository(
    private val messageDao: MessageDao,
    private val productDao: ProductDao
) : MessageRepository {

    override fun getMessages(productId: Int, buyerId: Long): Flow<List<Message>> =
        messageDao.getMessages(productId, buyerId).map { list -> list.map { it.toMessage() } }

    override fun getConversations(userId: Long): Flow<List<Conversation>> =
        messageDao.getConversations(userId)

    override suspend fun sendMessage(
        productId: Int,
        buyerId: Long,
        senderId: Long,
        text: String
    ): SendMessageResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return SendMessageResult.Error(ChatErrors.EMPTY)
        if (trimmed.length > ChatErrors.MAX_LENGTH) return SendMessageResult.Error(ChatErrors.TOO_LONG)

        val product = productDao.getProductEntityById(productId)
            ?: return SendMessageResult.Error(ChatErrors.PRODUCT_NOT_FOUND)
        if (buyerId == product.sellerId) return SendMessageResult.Error(ChatErrors.OWN_PRODUCT)
        if (senderId != buyerId && senderId != product.sellerId) {
            return SendMessageResult.Error(ChatErrors.NOT_PARTICIPANT)
        }

        messageDao.insert(
            MessageEntity(productId = productId, buyerId = buyerId, senderId = senderId, text = trimmed)
        )
        return SendMessageResult.Success
    }
}
