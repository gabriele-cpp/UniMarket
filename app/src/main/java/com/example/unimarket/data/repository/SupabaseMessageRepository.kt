package com.example.unimarket.data.repository

import android.content.Context
import com.example.unimarket.data.local.MessageDao
import com.example.unimarket.data.local.MessageEntity
import com.example.unimarket.data.local.ProductDao
import com.example.unimarket.data.local.UserDao
import com.example.unimarket.data.local.UserEntity
import com.example.unimarket.data.local.UserPreferences
import com.example.unimarket.data.model.Conversation
import com.example.unimarket.data.model.Message
import com.example.unimarket.data.remote.RealtimeMessageEvent
import com.example.unimarket.data.remote.SupabaseApi
import com.example.unimarket.data.remote.SupabaseMessageDto
import com.example.unimarket.data.remote.SupabaseRealtime
import com.example.unimarket.ui.util.NotificationHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.OffsetDateTime

class SupabaseMessageRepository(
    private val supabaseApi: SupabaseApi,
    private val messageDao: MessageDao,
    private val productDao: ProductDao,
    private val userDao: UserDao,
    private val coroutineScope: CoroutineScope,
    private val context: Context,
    private val userPreferences: UserPreferences,
    /** Null = tanpa realtime (pesan baru hanya diambil saat layar dibuka). */
    private val realtime: SupabaseRealtime? = null
) : MessageRepository {

    private val offlineRepo = OfflineMessageRepository(messageDao, productDao, userPreferences)
    private val saveMutex = Mutex()

    /** Pesan dari HP ini yang sedang dikirim dan belum dicocokkan dengan versi server. Dijaga [saveMutex]. */
    private val pendingSends = mutableListOf<PendingSend>()

    private data class PendingSend(
        val productId: Int,
        val senderEmail: String,
        val text: String,
        val localId: Long
    )

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

    override fun observeUnreadCount(userId: Long): Flow<Int> =
        offlineRepo.observeUnreadCount(userId)

    override fun markMessagesAsRead() {
        offlineRepo.markMessagesAsRead()
    }

    override suspend fun sendMessage(
        productId: Int,
        buyerId: Long,
        senderId: Long,
        text: String
    ): SendMessageResult {
        // 1. Validasi & simpan lokal terlebih dahulu agar UI responsif instan
        val local = when (val result = offlineRepo.insertLocalMessage(productId, buyerId, senderId, text)) {
            is LocalInsertResult.Rejected -> return SendMessageResult.Error(result.message)
            is LocalInsertResult.Saved -> result
        }

        // 2. Kirim ke Supabase
        withContext(Dispatchers.IO) {
            val buyer = userDao.getUserById(buyerId)
            val sender = userDao.getUserById(senderId)
            if (buyer == null || sender == null) return@withContext

            // Catat dulu sebelum mengirim: event realtime untuk pesan ini bisa tiba sebelum
            // respons REST, dan harus dikenali sebagai pesan yang sudah ada di layar.
            val pending = PendingSend(productId, sender.email, local.text, local.localId)
            saveMutex.withLock { pendingSends += pending }
            try {
                val dto = SupabaseMessageDto(
                    product_id = productId,
                    buyer_email = buyer.email,
                    sender_email = sender.email,
                    buyer_name = buyer.name,
                    sender_name = sender.name,
                    text = local.text
                )
                val saved = supabaseApi.insertMessage(dto).firstOrNull()
                if (saved != null) {
                    saveMutex.withLock { saveRemoteMessage(saved, currentUserEmail = null, notify = false) }
                }
                android.util.Log.d("SupabaseMessage", "Pesan terkirim ke Supabase untuk produk $productId")
            } catch (e: Exception) {
                android.util.Log.e("SupabaseMessage", "Gagal kirim pesan ke Supabase: ${e.message}")
            } finally {
                // Jika gagal terkirim, jangan biarkan catatan ini cocok dengan pesan lain nanti.
                withContext(NonCancellable) { saveMutex.withLock { pendingSends -= pending } }
            }
        }
        return SendMessageResult.Success
    }

    override fun realtimeUpdates(productId: Int): Flow<Unit> {
        val client = realtime ?: return emptyFlow()
        var isFirstSubscription = true
        return client.messageInserts(productId).map<RealtimeMessageEvent, Unit> { event ->
            when (event) {
                is RealtimeMessageEvent.Subscribed -> {
                    // Saat layar dibuka, getMessages() sudah sinkron. Setelah tersambung ulang
                    // (mis. sinyal sempat hilang), ambil pesan yang terlewat selama terputus.
                    if (!isFirstSubscription) syncMessages(productId)
                    isFirstSubscription = false
                }
                is RealtimeMessageEvent.Inserted -> try {
                    saveMutex.withLock {
                        // Layar chat sedang terbuka, jadi tidak perlu notifikasi.
                        saveRemoteMessage(event.message, currentUserEmail = null, notify = false)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    android.util.Log.e("SupabaseMessage", "Gagal simpan pesan realtime: ${e.message}")
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    private suspend fun syncMessages(productId: Int?) {
        try {
            val filter = if (productId != null) "eq.$productId" else null
            val remoteMessages = supabaseApi.getMessages(filter)

            val currentUserId = userPreferences.getLoggedInUserId()
            var currentUserEmail: String? = null
            if (currentUserId != null) {
                currentUserEmail = userDao.getUserById(currentUserId)?.email
            }

            saveMutex.withLock {
                for (dto in remoteMessages) {
                    saveRemoteMessage(dto, currentUserEmail, notify = true)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            android.util.Log.e("SupabaseMessage", "Gagal sinkron pesan: ${e.message}")
        }
    }

    /**
     * Menyimpan satu pesan dari Supabase ke Room jika belum ada.
     * Harus dipanggil di dalam [saveMutex] agar sinkronisasi penuh dan realtime tidak
     * menyimpan pesan yang sama dua kali.
     */
    private suspend fun saveRemoteMessage(dto: SupabaseMessageDto, currentUserEmail: String?, notify: Boolean) {
        val timestamp = parseCreatedAt(dto.created_at)
        // Cek apakah pesan sudah ada di Room lokal
        if (messageDao.exists(dto.product_id, dto.text, timestamp) > 0) return

        // Pesan yang baru saja dikirim dari HP ini sudah tampil (tersimpan dengan jam HP).
        // Cocokkan lewat id lokalnya, bukan jam, supaya tetap benar walau jam HP tidak akurat,
        // lalu samakan jamnya dengan jam server alih-alih menyimpan salinan kedua.
        val pending = pendingSends.firstOrNull {
            it.productId == dto.product_id && it.senderEmail == dto.sender_email && it.text == dto.text
        }
        if (pending != null) {
            pendingSends -= pending
            messageDao.updateTimestamp(pending.localId, timestamp)
            return
        }

        // Dapatkan atau buat user pembeli & pengirim lokal
        val localBuyerId = getOrCreateUserId(dto.buyer_name ?: "Pembeli", dto.buyer_email)
        val localSenderId = getOrCreateUserId(dto.sender_name ?: "Pengirim", dto.sender_email)

        // Cadangan untuk pesan yang terkirim tapi belum sempat dicocokkan (mis. aplikasi
        // ditutup sebelum respons server tiba): cari salinan lokal di sekitar jam server.
        val localCopy = messageDao.findLocalCopy(
            productId = dto.product_id,
            buyerId = localBuyerId,
            senderId = localSenderId,
            text = dto.text,
            from = timestamp - LOCAL_COPY_WINDOW_MS,
            to = timestamp + LOCAL_COPY_WINDOW_MS,
            around = timestamp
        )
        if (localCopy != null) {
            messageDao.updateTimestamp(localCopy.id, timestamp)
            return
        }

        // Pastikan produk lokal ada sebelum insert foreign key
        if (productDao.getProductEntityById(dto.product_id) == null) return
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

        // Kirim notifikasi jika pesan baru BUKAN dari user saat ini
        if (notify && currentUserEmail != null && dto.sender_email != currentUserEmail) {
            NotificationHelper.showChatNotification(
                context = context,
                senderName = dto.sender_name ?: "Seseorang",
                messageText = dto.text,
                productId = dto.product_id,
                buyerId = localBuyerId
            )
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
        // Supabase mengirim "2026-11-26T01:56:30.743916+00:00". Instant.parse di Android lama
        // tidak menerima offset "+00:00", jadi coba OffsetDateTime dulu.
        return try {
            OffsetDateTime.parse(dateStr).toInstant().toEpochMilli()
        } catch (e: Exception) {
            try {
                Instant.parse(dateStr).toEpochMilli()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private companion object {
        /** Selisih maksimal jam HP dan jam server saat mencocokkan pesan kiriman sendiri. */
        const val LOCAL_COPY_WINDOW_MS = 5 * 60 * 1000L
    }
}
