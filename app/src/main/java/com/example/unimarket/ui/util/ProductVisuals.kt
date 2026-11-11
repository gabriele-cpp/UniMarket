package com.example.unimarket.ui.util

import androidx.compose.ui.graphics.Color
import java.io.File

/** Label opsi filter "semua kategori". Hanya ada di UI, bukan baris di tabel categories. */
const val ALL_CATEGORIES_LABEL = "Semua"

/** Palet latar placeholder gambar. Warna dipilih dari id kategori, jadi kategori baru otomatis kebagian warna. */
private val categoryPalette = listOf(
    Color(0xFFD7E7F5),
    Color(0xFFE9DFB9),
    Color(0xFFE6E1ED),
    Color(0xFFD9EAD8),
    Color(0xFFF5DCD7),
    Color(0xFFDCEFF0)
)

/** Warna latar placeholder kartu/detail berdasarkan id kategori. */
fun categoryColor(categoryId: Int): Color =
    categoryPalette[Math.floorMod(categoryId - 1, categoryPalette.size)]

/** Inisial 2 huruf untuk placeholder gambar, contoh "Scientific Calculator" -> "SC". */
fun productMark(title: String): String {
    val words = title.trim().split(Regex("\\s+")).filter { w -> w.firstOrNull()?.isLetterOrDigit() == true }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "UM"
    }
}

/**
 * Model untuk Coil AsyncImage. Foto tersimpan berupa path file absolut ("/data/...")
 * sehingga dibungkus File; URI content:// dari photo picker (preview) dipakai apa adanya.
 */
fun imageModel(imageUri: String?): Any? = when {
    imageUri.isNullOrBlank() -> null
    imageUri.startsWith("/") -> File(imageUri)
    else -> imageUri
}
