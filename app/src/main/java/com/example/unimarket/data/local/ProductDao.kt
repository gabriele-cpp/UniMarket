package com.example.unimarket.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.unimarket.data.model.ProductStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    /**
     * Barang untuk katalog publik (Home). Barang dengan status [excludedStatus] (TERJUAL) tidak ikut.
     * categoryId null = semua kategori.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM products
        WHERE status != :excludedStatus
          AND (:categoryId IS NULL OR categoryId = :categoryId)
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun getMarketplaceProducts(categoryId: Int?, excludedStatus: ProductStatus): Flow<List<ProductWithSeller>>

    /**
     * Pencarian katalog publik berdasarkan kata kunci (judul, deskripsi, nama kategori)
     * dan kategori opsional. Barang TERJUAL tidak ikut.
     * [sort] = nama [com.example.unimarket.data.model.ProductSort]: NEWEST, PRICE_ASC, PRICE_DESC.
     */
    @Transaction
    @Query(
        """
        SELECT products.* FROM products
        INNER JOIN categories ON categories.id = products.categoryId
        WHERE products.status != :excludedStatus
          AND (:categoryId IS NULL OR products.categoryId = :categoryId)
          AND (
            :query = ''
            OR products.title LIKE '%' || :query || '%'
            OR products.description LIKE '%' || :query || '%'
            OR categories.name LIKE '%' || :query || '%'
          )
        ORDER BY
          CASE WHEN :sort = 'PRICE_ASC' THEN products.price END ASC,
          CASE WHEN :sort = 'PRICE_DESC' THEN products.price END DESC,
          products.createdAt DESC,
          products.id DESC
        """
    )
    fun searchProducts(
        query: String,
        categoryId: Int?,
        excludedStatus: ProductStatus,
        sort: String
    ): Flow<List<ProductWithSeller>>

    /** Semua barang milik seorang penjual, termasuk yang sudah terjual (untuk My Items). */
    @Transaction
    @Query("SELECT * FROM products WHERE sellerId = :sellerId ORDER BY createdAt DESC, id DESC")
    fun getProductsBySeller(sellerId: Long): Flow<List<ProductWithSeller>>

    @Query("SELECT COUNT(*) FROM products WHERE sellerId = :sellerId AND status = :status")
    fun countBySellerAndStatus(sellerId: Long, status: ProductStatus): Flow<Int>

    @Transaction
    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Int): ProductWithSeller?

    @Transaction
    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    fun observeProductById(productId: Int): Flow<ProductWithSeller?>

    /** Entity mentah, dipakai untuk update/delete. */
    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductEntityById(productId: Int): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    /**
     * Ubah status hanya jika barang milik [sellerId].
     * @return jumlah baris yang berubah (0 = bukan pemilik / barang tidak ada).
     */
    @Query("UPDATE products SET status = :status WHERE id = :productId AND sellerId = :sellerId")
    suspend fun updateStatus(productId: Int, sellerId: Long, status: ProductStatus): Int

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}
