package com.example.unimarket.data.repository

import com.example.unimarket.data.local.MessageDao
import com.example.unimarket.data.local.MessageEntity
import com.example.unimarket.data.local.ProductDao
import com.example.unimarket.data.local.UserDao
import com.example.unimarket.data.local.UserEntity
import com.example.unimarket.data.model.Conversation
import com.example.unimarket.data.model.Message
import com.example.unimarket.data.remote.SupabaseApi
import com.example.unimarket.data.remote.SupabaseMessageDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class SupabaseMessageRepository(
    private val supabaseApi: SupabaseApi,
    private val messageDao: MessageDao,
    private val productDao: ProductDao,
    private val userDao: UserDao,
    private val coroutineScope: CoroutineScope
) : MessageRepository {

    private val offlineRepo = OfflineMessageRepository(messageDao, productDao)

    override fun getMessages(productId: Int, buyerId: Long): Flow<List<Message>> {
        // Ambil pesan terbaru dari Supabase di background
        coroutineScope.launch(Dispatchers.IO) {
            syncMessages(productId)
        }
        return offlineRepo.getMessages(productId, buyerId)
    }

    override fun getConversations(userId: Long): Flow<List<Conversation>> {
        // Ambil semua pesan percakapan dari Supabase di background
        coroutineScope.launch(Dispatchers.IO) {
            syncMessages(null)
        }
        return offlineRepo.getConversations(userId)
    }

    override suspend fun sendMessage(
        productId: Int,
        buyerId: Long,
        senderId: Long,
        text: String
    ): SendMessageResult {
        // 1. Validasi & simpan lokal terlebih dahulu agar UI responsif instan
        val localResult = offlineRepo.sendMessage(productId, buyerId, senderId, text)
        if (localResult !is SendMessageResult.Success) {
            return localResult
        }

        // 2. Kirim ke Supabase
        withContext(Dispatchers.IO) {
            try {
                val buyer = userDao.getUserById(buyerId)
                val sender = userDao.getUserById(senderId)
                if (buyer != null && sender != null) {
                    val dto = SupabaseMessageDto(
                        product_id = productId,
                        buyer_email = buyer.email,
                        sender_email = sender.email,
                        buyer_name = buyer.name,
                        sender_name = sender.name,
                        text = text.trim()
                    )
                    supabaseApi.insertMessage(dto)
                    android.util.Log.d("SupabaseMessage", "Pesan terkirim ke Supabase untuk produk $productId")
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseMessage", "Gagal kirim pesan ke Supabase: ${e.message}")
            }
        }
        return SendMessageResult.Success
    }

    private suspend fun syncMessages(productId: Int?) {
        try {
            val filter = if (productId != null) "eq.$productId" else null
            val remoteMessages = supabaseApi.getMessages(filter)
            for (dto in remoteMessages) {
                val timestamp = parseCreatedAt(dto.created_at)
                // Cek apakah pesan sudah ada di Room lokal
                if (messageDao.exists(dto.product_id, dto.text, timestamp) > 0) {
                    continue
                }

                // Dapatkan atau buat user pembeli & pengirim lokal
                val localBuyerId = getOrCreateUserId(dto.buyer_name ?: "Pembeli", dto.buyer_email)
                val localSenderId = getOrCreateUserId(dto.sender_name ?: "Pengirim", dto.sender_email)

                // Pastikan produk lokal ada sebelum insert foreign key
                val product = productDao.getProductEntityById(dto.product_id)
                if (product != null) {
                    messageDao.insert(
                        MessageEntity(
                            id = 0,
                            productId = dto.product_id,
                            buyerId = localBuyerId,
                            senderId = localSenderId,
                            text = dto.text,
                            timestamp = timestamp
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseMessage", "Gagal sinkron pesan: ${e.message}")
        }
    }

    private suspend fun getOrCreateUserId(name: String, email: String): Long {
        val existing = userDao.getUserByEmail(email)
        if (existing != null) return existing.id

        val newUser = UserEntity(
            name = name,
            email = email,
            passwordHash = "",
            phone = ""
        )
        return userDao.upsertUser(newUser)
    }

    private fun parseCreatedAt(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            Instant.parse(dateStr).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
