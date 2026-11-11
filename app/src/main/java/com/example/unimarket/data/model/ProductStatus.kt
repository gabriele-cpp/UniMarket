package com.example.unimarket.data.model

/**
 * Status ketersediaan barang. Disimpan di Room sebagai String (lihat Converters).
 */
enum class ProductStatus(val label: String) {
    TERSEDIA("Tersedia"),
    NEGOSIASI("Negosiasi"),
    TERJUAL("Terjual")
}
