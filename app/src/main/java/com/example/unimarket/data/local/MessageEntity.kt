package com.example.unimarket.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.unimarket.data.model.Message

/**
 * Satu pesan chat. Satu percakapan = kombinasi (productId, buyerId):
 * pembeli [buyerId] berbicara dengan penjual barang [productId].
 * [senderId] bisa pembeli atau penjual.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["buyerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["senderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        // Mengambil isi satu percakapan secara berurutan
        Index(value = ["productId", "buyerId", "timestamp"]),
        Index(value = ["buyerId"]),
        Index(value = ["senderId"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Int,
    val buyerId: Long,
    val senderId: Long,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMessage(): Message = Message(
        id = id,
        productId = productId,
        buyerId = buyerId,
        senderId = senderId,
        text = text,
        timestamp = timestamp
    )
}
