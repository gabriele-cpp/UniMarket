package com.example.unimarket.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.unimarket.data.model.Order
import com.example.unimarket.data.model.OrderStatus

/**
 * Pesanan pembelian satu barang oleh [buyerId], dibayar lewat Midtrans Snap.
 * Jika barang atau pembeli dihapus, pesanannya ikut terhapus (CASCADE).
 */
@Entity(
    tableName = "orders",
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
        )
    ],
    indices = [
        Index(value = ["orderId"], unique = true),
        Index(value = ["productId"]),
        Index(value = ["buyerId"])
    ]
)
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** order_id Midtrans, format "UM-<timestamp>-<productId>". */
    val orderId: String,
    val productId: Int,
    val buyerId: Long,
    /** Harga barang saat pesanan dibuat, dalam Rupiah. */
    val amount: Long,
    val quantity: Int = 1,
    val status: OrderStatus = OrderStatus.PENDING,
    val redirectUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** Pesanan beserta barang (dan penjualnya), untuk Riwayat Pembelian. */
data class OrderWithProduct(
    @Embedded val order: OrderEntity,
    @Relation(entity = ProductEntity::class, parentColumn = "productId", entityColumn = "id")
    val product: ProductWithSeller?
) {
    fun toOrder(): Order = Order(
        id = order.id,
        orderId = order.orderId,
        productId = order.productId,
        productTitle = product?.product?.title ?: "Barang tidak ditemukan",
        productImageUri = product?.product?.imageUri,
        sellerName = product?.seller?.name ?: "Penjual tidak diketahui",
        buyerId = order.buyerId,
        amount = order.amount,
        quantity = order.quantity,
        status = order.status,
        redirectUrl = order.redirectUrl,
        createdAt = order.createdAt
    )
}
