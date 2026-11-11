package com.example.unimarket.data.model

/**
 * Nilai default yang dipakai bersama oleh Product (UI) dan ProductEntity (Room).
 */
object ProductDefaults {
    const val LOCATION = "Area Kampus"
    const val DELIVERY_METHOD = "COD Kampus"

    /** Pilihan kondisi barang (dipakai form Post dan data seed, agar konsisten). */
    val CONDITIONS = listOf("Baru", "Sangat baik", "Baik", "Minus minor")
    const val DEFAULT_CONDITION = "Sangat baik"
}

/** Urutan hasil pencarian. */
enum class ProductSort(val label: String) {
    NEWEST("Terbaru"),
    PRICE_ASC("Harga termurah"),
    PRICE_DESC("Harga termahal")
}

/**
 * Model barang untuk lapisan UI, sudah digabung dengan data penjual & kategori.
 * Tidak bergantung pada Compose: warna/inisial kartu ditentukan di lapisan UI.
 */
data class Product(
    val id: Int,
    val title: String,
    /** Harga dalam Rupiah tanpa format, contoh 85000. Format tampilan: formatRupiah(). */
    val price: Long,
    val categoryId: Int,
    val categoryName: String,
    val condition: String,
    val sellerId: Long,
    val sellerName: String,
    val sellerPhone: String,
    val description: String,
    val imageUri: String? = null,
    val location: String = ProductDefaults.LOCATION,
    val deliveryMethod: String = ProductDefaults.DELIVERY_METHOD,
    val status: ProductStatus = ProductStatus.TERSEDIA,
    val createdAt: Long = 0L
)

/**
 * Data yang dibutuhkan untuk membuat barang baru (dari form Post Item).
 */
data class ProductInput(
    val title: String,
    val price: Long,
    val categoryId: Int,
    val condition: String,
    val sellerId: Long,
    val description: String,
    val imageUri: String? = null,
    val location: String = ProductDefaults.LOCATION,
    val deliveryMethod: String = ProductDefaults.DELIVERY_METHOD
)
