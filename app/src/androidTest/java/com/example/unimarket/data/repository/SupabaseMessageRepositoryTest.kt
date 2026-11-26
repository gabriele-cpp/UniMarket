package com.example.unimarket.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.unimarket.data.local.MessageEntity
import com.example.unimarket.data.local.UniMarketDatabase
import com.example.unimarket.data.local.UserPreferences
import com.example.unimarket.data.remote.SupabaseApi
import com.example.unimarket.data.remote.SupabaseAuthResponse
import com.example.unimarket.data.remote.SupabaseLoginRequest
import com.example.unimarket.data.remote.SupabaseMessageDto
import com.example.unimarket.data.remote.SupabaseProductDto
import com.example.unimarket.data.remote.SupabaseProfileDto
import com.example.unimarket.data.remote.SupabaseSignUpRequest
import com.example.unimarket.data.remote.SupabaseStorageUploadResponse
import com.example.unimarket.data.remote.SupabaseUpdateProfileRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.RequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Regresi bug "pesan terkirim dua kali": pesan yang dikirim dari HP ini sudah tersimpan lokal
 * (jam HP), lalu versi server (jam server) datang lagi lewat respons REST / sinkronisasi.
 * Keduanya harus digabung menjadi satu pesan, walaupun jam HP dan server berbeda jauh.
 *
 * Memakai Room in-memory dan SupabaseApi palsu, jadi tidak menyentuh Supabase sungguhan.
 */
@RunWith(AndroidJUnit4::class)
class SupabaseMessageRepositoryTest {

    private lateinit var db: UniMarketDatabase
    private lateinit var api: FakeSupabaseApi
    private lateinit var scope: CoroutineScope
    private lateinit var repository: SupabaseMessageRepository
    private lateinit var seed: Seed
    private var productId = 0

    @Before
    fun setUp() = runBlocking {
        db = inMemoryDatabase()
        seed = db.seedBasics()
        productId = db.insertProduct(seed.sellerId, "Kalkulator")
        api = FakeSupabaseApi()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repository = SupabaseMessageRepository(
            supabaseApi = api,
            messageDao = db.messageDao(),
            productDao = db.productDao(),
            userDao = db.userDao(),
            coroutineScope = scope,
            context = targetContext,
            userPreferences = UserPreferences(targetContext),
            realtime = null
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    private suspend fun localMessages() = db.messageDao().getMessages(productId, seed.buyerId).first()

    /** Menunggu sinkronisasi latar (getConversations) sampai [done] terpenuhi. */
    private suspend fun awaitLocal(done: (List<MessageEntity>) -> Boolean): List<MessageEntity> =
        withTimeout(5_000) { db.messageDao().getMessages(productId, seed.buyerId).first(done) }

    @Test
    fun ownMessage_withServerClockFarAhead_isStoredOnce_withServerTime() = runBlocking {
        api.serverClockOffsetMs = 10 * 60 * 1000L   // jam server 10 menit lebih cepat dari HP

        repository.sendMessage(productId, seed.buyerId, seed.buyerId, "p")

        val messages = localMessages()
        assertEquals(1, messages.size)
        assertEquals("p", messages.single().text)
        assertEquals(api.lastCreatedAtMillis, messages.single().timestamp)
    }

    @Test
    fun ownMessage_stillSingle_afterLaterFullSync() = runBlocking {
        api.serverClockOffsetMs = 10 * 60 * 1000L
        repository.sendMessage(productId, seed.buyerId, seed.buyerId, "p")

        // Membuka daftar chat memicu sinkronisasi penuh dari server.
        repository.getConversations(seed.buyerId)
        api.awaitGetMessagesCalls(1)
        Thread.sleep(300)

        assertEquals(1, localMessages().size)
    }

    @Test
    fun ownMessage_syncArrivingBeforeRestResponse_isNotDuplicated() = runBlocking {
        api.serverClockOffsetMs = 10 * 60 * 1000L
        // Sinkronisasi berjalan saat request kirim pesan belum selesai (kondisi balapan
        // yang sama dengan event realtime yang tiba sebelum respons REST).
        api.beforeInsertReturns = {
            repository.getConversations(seed.buyerId)
            awaitLocal { list -> list.singleOrNull()?.timestamp == api.lastCreatedAtMillis }
        }

        repository.sendMessage(productId, seed.buyerId, seed.buyerId, "p")

        val messages = localMessages()
        assertEquals(1, messages.size)
        assertEquals(api.lastCreatedAtMillis, messages.single().timestamp)
    }

    @Test
    fun sameTextSentTwice_keepsBothMessages() = runBlocking {
        repository.sendMessage(productId, seed.buyerId, seed.buyerId, "p")
        repository.sendMessage(productId, seed.buyerId, seed.buyerId, "p")

        assertEquals(listOf("p", "p"), localMessages().map { it.text })
    }

    @Test
    fun messageFromOtherSide_isSavedOnce_evenWhenSyncedTwice() = runBlocking {
        api.addServerMessage(senderEmail = "sari@kampus.ac.id", senderName = "Sari Penjual", text = "Masih ada, kak")

        repository.getConversations(seed.buyerId)
        val afterFirstSync = awaitLocal { it.isNotEmpty() }
        repository.getConversations(seed.buyerId)
        api.awaitGetMessagesCalls(2)
        Thread.sleep(300)

        assertEquals(1, afterFirstSync.size)
        assertEquals(listOf("Masih ada, kak"), localMessages().map { it.text })
        assertEquals(seed.sellerId, localMessages().single().senderId)
    }

    @Test
    fun serverFailure_keepsLocalMessage() = runBlocking {
        api.failInsert = true

        val result = repository.sendMessage(productId, seed.buyerId, seed.buyerId, "halo")

        assertEquals(SendMessageResult.Success, result)
        assertEquals(listOf("halo"), localMessages().map { it.text })
    }

    @Test
    fun invalidMessage_isNotSentToServer() = runBlocking {
        val result = repository.sendMessage(productId, seed.buyerId, seed.otherUserId, "nimbrung")

        assertEquals(SendMessageResult.Error(ChatErrors.NOT_PARTICIPANT), result)
        assertEquals(0, api.insertCalls)
    }

    /** Server Supabase palsu untuk tabel messages. Endpoint lain tidak dipakai repository ini. */
    private inner class FakeSupabaseApi : SupabaseApi {
        private val serverMessages = mutableListOf<SupabaseMessageDto>()
        var serverClockOffsetMs = 0L
        var failInsert = false
        var insertCalls = 0
        var lastCreatedAtMillis = 0L
        var beforeInsertReturns: (suspend () -> Unit)? = null
        @Volatile private var getMessagesCalls = 0

        fun addServerMessage(senderEmail: String, senderName: String, text: String) {
            serverMessages += dto(senderEmail, senderName, text)
        }

        suspend fun awaitGetMessagesCalls(count: Int) = withTimeout(5_000) {
            while (getMessagesCalls < count) kotlinx.coroutines.delay(20)
        }

        private fun dto(senderEmail: String, senderName: String, text: String): SupabaseMessageDto {
            val createdAt = System.currentTimeMillis() + serverClockOffsetMs
            lastCreatedAtMillis = createdAt
            return SupabaseMessageDto(
                id = serverMessages.size + 1L,
                product_id = productId,
                buyer_email = "bima@kampus.ac.id",
                sender_email = senderEmail,
                buyer_name = "Bima Pembeli",
                sender_name = senderName,
                text = text,
                // Format asli Supabase: mikrodetik + offset "+00:00"
                created_at = Instant.ofEpochMilli(createdAt).atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'000'xxx"))
            )
        }

        override suspend fun getMessages(productIdFilter: String?): List<SupabaseMessageDto> =
            synchronized(serverMessages) { serverMessages.toList() }.also { getMessagesCalls++ }

        override suspend fun insertMessage(message: SupabaseMessageDto, prefer: String): List<SupabaseMessageDto> {
            insertCalls++
            if (failInsert) throw IOException("Tidak ada koneksi")
            val saved = dto(message.sender_email, message.sender_name.orEmpty(), message.text)
            synchronized(serverMessages) { serverMessages += saved }
            beforeInsertReturns?.invoke()
            return listOf(saved)
        }

        override suspend fun signUp(request: SupabaseSignUpRequest): SupabaseAuthResponse = unused()
        override suspend fun login(request: SupabaseLoginRequest): SupabaseAuthResponse = unused()
        override suspend fun getProfile(idFilter: String): List<SupabaseProfileDto> = unused()
        override suspend fun updateProfile(idFilter: String, body: SupabaseUpdateProfileRequest, prefer: String): List<SupabaseProfileDto> = unused()
        override suspend fun getProducts(categoryFilter: String?): List<SupabaseProductDto> = unused()
        override suspend fun searchProducts(orFilter: String): List<SupabaseProductDto> = unused()
        override suspend fun getProductById(idFilter: String): List<SupabaseProductDto> = unused()
        override suspend fun insertProduct(product: SupabaseProductDto, prefer: String): List<SupabaseProductDto> = unused()
        override suspend fun updateProduct(idFilter: String, body: Map<String, Any>, prefer: String): List<SupabaseProductDto> = unused()
        override suspend fun deleteProduct(idFilter: String) = unused()
        override suspend fun uploadImage(path: String, imageBytes: RequestBody, upsert: String): SupabaseStorageUploadResponse = unused()

        private fun unused(): Nothing = throw UnsupportedOperationException("Tidak dipakai di test chat")
    }
}
