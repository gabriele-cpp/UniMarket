package com.example.unimarket.data.repository

import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.ProductInput
import com.example.unimarket.data.model.ProductSort
import com.example.unimarket.data.model.ProductStatus
import kotlinx.coroutines.flow.Flow

/**
 * Sumber data barang. Implementasi utama: OfflineProductRepository (Room).
 * Untuk testing bisa dibuat FakeProductRepository yang mengimplementasikan interface ini.
 */
interface ProductRepository {
    /** Katalog publik (tanpa barang TERJUAL). categoryId null = semua kategori. */
    fun getProductsByCategory(categoryId: Int?): Flow<List<Product>>

    /** Pencarian katalog publik (tanpa barang TERJUAL). query kosong = tanpa kata kunci. */
    fun searchProducts(
        query: String,
        categoryId: Int?,
        sort: ProductSort = ProductSort.NEWEST
    ): Flow<List<Product>>

    /** Semua barang milik penjual, termasuk yang TERJUAL. */
    fun getProductsBySeller(sellerId: Long): Flow<List<Product>>

    fun countBySellerAndStatus(sellerId: Long, status: ProductStatus): Flow<Int>

    fun observeProduct(id: Int): Flow<Product?>

    suspend fun getProductById(id: Int): Product?

    /**
     * Mengembalikan id barang baru. Jika input.imageUri berupa URI dari photo picker,
     * foto disalin dulu ke penyimpanan internal (lihat ImageStorage).
     */
    suspend fun addProduct(input: ProductInput): Long

    /**
     * Mengubah data barang. Status dan tanggal dibuat tidak berubah.
     * Hanya berhasil jika input.sellerId adalah pemilik barang.
     */
    suspend fun updateProduct(productId: Int, input: ProductInput): OwnershipResult

    /** Hanya berhasil jika [requesterId] adalah pemilik barang. */
    suspend fun updateStatus(productId: Int, requesterId: Long, status: ProductStatus): OwnershipResult

    /** Hanya berhasil jika [requesterId] adalah pemilik barang. */
    suspend fun deleteProduct(productId: Int, requesterId: Long): OwnershipResult
}

/** Hasil operasi yang hanya boleh dilakukan pemilik barang. */
enum class OwnershipResult {
    SUCCESS,
    NOT_FOUND,
    NOT_OWNER;

    val message: String?
        get() = when (this) {
            SUCCESS -> null
            NOT_FOUND -> "Barang tidak ditemukan"
            NOT_OWNER -> "Anda hanya bisa mengubah barang milik sendiri"
        }
}
