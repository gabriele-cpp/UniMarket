package com.example.unimarket.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.unimarket.data.model.OrderStatus
import com.example.unimarket.data.model.ProductStatus
import kotlinx.coroutines.flow.Flow

/**
 * Abstract class (bukan interface) agar [markPaid] bisa berisi beberapa query
 * dalam satu transaksi database.
 */
@Dao
abstract class OrderDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(order: OrderEntity): Long

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    abstract suspend fun getByOrderId(orderId: String): OrderEntity?

    @Transaction
    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    abstract suspend fun getWithProduct(orderId: String): OrderWithProduct?

    @Transaction
    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    abstract fun observeWithProduct(orderId: String): Flow<OrderWithProduct?>

    /** Riwayat pembelian seorang pembeli, terbaru dulu. */
    @Transaction
    @Query("SELECT * FROM orders WHERE buyerId = :buyerId ORDER BY createdAt DESC, id DESC")
    abstract fun observeByBuyer(buyerId: Long): Flow<List<OrderWithProduct>>

    @Query("UPDATE orders SET redirectUrl = :redirectUrl WHERE orderId = :orderId")
    abstract suspend fun updateRedirectUrl(orderId: String, redirectUrl: String): Int

    @Query("UPDATE orders SET status = :status WHERE orderId = :orderId")
    abstract suspend fun updateStatus(orderId: String, status: OrderStatus): Int

    @Query("DELETE FROM orders WHERE orderId = :orderId")
    abstract suspend fun deleteByOrderId(orderId: String): Int

    /**
     * Ubah status barang tanpa cek pemilik. Hanya dipakai [markPaid]: yang menandai
     * barang terjual adalah pembayaran pembeli, bukan aksi penjual.
     */
    @Query("UPDATE products SET status = :status WHERE id = :productId")
    protected abstract suspend fun setProductStatus(productId: Int, status: ProductStatus): Int

    /** Pesanan lunas dan barangnya otomatis TERJUAL, dalam satu transaksi. */
    @Transaction
    open suspend fun markPaid(orderId: String, productId: Int) {
        updateStatus(orderId, OrderStatus.PAID)
        setProductStatus(productId, ProductStatus.TERJUAL)
    }
}
