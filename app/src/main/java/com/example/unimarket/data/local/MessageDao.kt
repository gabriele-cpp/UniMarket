package com.example.unimarket.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.unimarket.data.model.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    /** Isi satu percakapan (barang + pembeli), dari yang paling lama. */
    @Query(
        """
        SELECT * FROM messages
        WHERE productId = :productId AND buyerId = :buyerId
        ORDER BY timestamp ASC, id ASC
        """
    )
    fun getMessages(productId: Int, buyerId: Long): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(message: MessageEntity): Long

    /**
     * Daftar percakapan di mana [userId] adalah pembeli ATAU penjual barang.
     * Satu baris per percakapan (productId + buyerId), memakai pesan terakhirnya.
     * "Lawan bicara" = penjual jika user adalah pembeli, dan sebaliknya.
     */
    @Query(
        """
        SELECT
            m.productId AS productId,
            m.buyerId AS buyerId,
            p.title AS productTitle,
            p.imageUri AS productImageUri,
            CASE WHEN m.buyerId = :userId THEN seller.name ELSE buyer.name END AS otherUserName,
            m.text AS lastMessage,
            m.timestamp AS lastTimestamp,
            m.senderId AS lastSenderId
        FROM messages m
        INNER JOIN products p ON p.id = m.productId
        INNER JOIN users buyer ON buyer.id = m.buyerId
        INNER JOIN users seller ON seller.id = p.sellerId
        WHERE (m.buyerId = :userId OR p.sellerId = :userId)
          AND m.id = (
            SELECT m2.id FROM messages m2
            WHERE m2.productId = m.productId AND m2.buyerId = m.buyerId
            ORDER BY m2.timestamp DESC, m2.id DESC
            LIMIT 1
          )
        ORDER BY m.timestamp DESC
        """
    )
    fun getConversations(userId: Long): Flow<List<Conversation>>
}
