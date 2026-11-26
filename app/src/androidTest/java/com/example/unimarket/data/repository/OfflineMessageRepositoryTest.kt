package com.example.unimarket.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.unimarket.data.local.MessageEntity
import com.example.unimarket.data.local.UniMarketDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test repository chat dengan Room in-memory: validasi pesan, siapa yang boleh mengirim,
 * daftar percakapan per user, dan jumlah pesan belum dibaca.
 */
@RunWith(AndroidJUnit4::class)
class OfflineMessageRepositoryTest {

    private lateinit var db: UniMarketDatabase
    private lateinit var repository: OfflineMessageRepository
    private lateinit var seed: Seed
    private var productId = 0

    @Before
    fun setUp() = runTest {
        db = inMemoryDatabase()
        repository = OfflineMessageRepository(db.messageDao(), db.productDao())
        seed = db.seedBasics()
        productId = db.insertProduct(seed.sellerId, "Kalkulator")
    }

    @After
    fun tearDown() = db.close()

    private suspend fun messages() = repository.getMessages(productId, seed.buyerId).first()

    @Test
    fun buyerAndSeller_canChat_andMessagesAreOrdered() = runTest {
        assertEquals(SendMessageResult.Success, repository.sendMessage(productId, seed.buyerId, seed.buyerId, "Masih ada?"))
        assertEquals(SendMessageResult.Success, repository.sendMessage(productId, seed.buyerId, seed.sellerId, "Masih, kak"))

        val list = messages()
        assertEquals(listOf("Masih ada?", "Masih, kak"), list.map { it.text })
        assertEquals(listOf(seed.buyerId, seed.sellerId), list.map { it.senderId })
    }

    @Test
    fun sendMessage_trimsWhitespace() = runTest {
        repository.sendMessage(productId, seed.buyerId, seed.buyerId, "   halo   ")

        assertEquals("halo", messages().single().text)
    }

    @Test
    fun sendMessage_rejectsEmptyAndTooLongText() = runTest {
        assertEquals(SendMessageResult.Error(ChatErrors.EMPTY), repository.sendMessage(productId, seed.buyerId, seed.buyerId, "   "))

        val tooLong = "a".repeat(ChatErrors.MAX_LENGTH + 1)
        assertEquals(SendMessageResult.Error(ChatErrors.TOO_LONG), repository.sendMessage(productId, seed.buyerId, seed.buyerId, tooLong))

        // Tepat di batas maksimal masih boleh.
        val maxLength = "a".repeat(ChatErrors.MAX_LENGTH)
        assertEquals(SendMessageResult.Success, repository.sendMessage(productId, seed.buyerId, seed.buyerId, maxLength))
        assertEquals(1, messages().size)
    }

    @Test
    fun sendMessage_rejectsSellerChattingOnOwnProduct() = runTest {
        val result = repository.sendMessage(productId, buyerId = seed.sellerId, senderId = seed.sellerId, text = "halo")

        assertEquals(SendMessageResult.Error(ChatErrors.OWN_PRODUCT), result)
    }

    @Test
    fun sendMessage_rejectsOutsiderJoiningConversation() = runTest {
        val result = repository.sendMessage(productId, seed.buyerId, senderId = seed.otherUserId, text = "ikut nimbrung")

        assertEquals(SendMessageResult.Error(ChatErrors.NOT_PARTICIPANT), result)
        assertTrue(messages().isEmpty())
    }

    @Test
    fun sendMessage_rejectsUnknownProduct() = runTest {
        val result = repository.sendMessage(999, seed.buyerId, seed.buyerId, "halo")

        assertEquals(SendMessageResult.Error(ChatErrors.PRODUCT_NOT_FOUND), result)
    }

    @Test
    fun conversations_showLastMessageAndOtherPartyName_forBothSides() = runTest {
        repository.sendMessage(productId, seed.buyerId, seed.buyerId, "Bisa COD?")
        db.messageDao().insert(MessageEntity(productId = productId, buyerId = seed.buyerId, senderId = seed.sellerId, text = "Bisa", timestamp = System.currentTimeMillis() + 1_000))

        val buyerView = repository.getConversations(seed.buyerId).first().single()
        val sellerView = repository.getConversations(seed.sellerId).first().single()

        assertEquals("Bisa", buyerView.lastMessage)
        assertEquals("Sari Penjual", buyerView.otherUserName)
        assertEquals("Bima Pembeli", sellerView.otherUserName)
        // User yang tidak terlibat tidak melihat percakapan ini.
        assertTrue(repository.getConversations(seed.otherUserId).first().isEmpty())
    }

    @Test
    fun unreadCount_countsOnlyMessagesFromOthers_afterLastRead() = runTest {
        val now = System.currentTimeMillis()
        val dao = db.messageDao()
        dao.insert(MessageEntity(productId = productId, buyerId = seed.buyerId, senderId = seed.buyerId, text = "Halo", timestamp = now))
        dao.insert(MessageEntity(productId = productId, buyerId = seed.buyerId, senderId = seed.sellerId, text = "Ya", timestamp = now + 1))
        dao.insert(MessageEntity(productId = productId, buyerId = seed.buyerId, senderId = seed.sellerId, text = "Mau?", timestamp = now + 2))

        // Penjual: pesan dari pembeli = 1. Pembeli: pesan dari penjual = 2.
        assertEquals(1, dao.observeUnreadCount(seed.sellerId, lastReadTimestamp = 0).first())
        assertEquals(2, dao.observeUnreadCount(seed.buyerId, lastReadTimestamp = 0).first())
        // Setelah dibaca sampai pesan "Ya", tinggal 1.
        assertEquals(1, dao.observeUnreadCount(seed.buyerId, lastReadTimestamp = now + 1).first())
    }
}
