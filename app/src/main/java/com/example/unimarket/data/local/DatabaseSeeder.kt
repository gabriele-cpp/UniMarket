package com.example.unimarket.data.local

import androidx.room.withTransaction
import com.example.unimarket.data.security.PasswordHasher

/**
 * Mengisi data awal (kategori, 1 user demo, 4 produk) jika tabel masih kosong.
 * Dipanggil sekali saat aplikasi dibuka (lihat UniMarketApplication).
 */
object DatabaseSeeder {
    /** Id user demo. Dipakai sementara sebagai sellerId sampai ada login sungguhan (Langkah 1B). */
    const val DEMO_USER_ID = 1L
    const val DEMO_EMAIL = "demo@mahasiswa.ac.id"
    const val DEMO_PASSWORD = "demo1234"

    private const val CAT_BUKU = 1
    private const val CAT_ELEKTRONIK = 2
    private const val CAT_PERLENGKAPAN = 3
    private const val CAT_ALAT_KULIAH = 4

    suspend fun seedIfEmpty(db: UniMarketDatabase) {
        db.withTransaction {
            val categoryDao = db.categoryDao()
            val userDao = db.userDao()
            val productDao = db.productDao()

            if (categoryDao.count() == 0) {
                categoryDao.insertAll(
                    listOf(
                        CategoryEntity(id = CAT_BUKU, name = "Buku"),
                        CategoryEntity(id = CAT_ELEKTRONIK, name = "Elektronik"),
                        CategoryEntity(id = CAT_PERLENGKAPAN, name = "Perlengkapan"),
                        CategoryEntity(id = CAT_ALAT_KULIAH, name = "Alat Kuliah")
                    )
                )
            }

            if (userDao.getUserById(DEMO_USER_ID) == null) {
                userDao.insertUser(
                    UserEntity(
                        id = DEMO_USER_ID,
                        name = "Mahasiswa Demo",
                        email = DEMO_EMAIL,
                        passwordHash = PasswordHasher.hash(DEMO_PASSWORD),
                        phone = "6281234567890"
                    )
                )
            }

            if (productDao.count() == 0) {
                val now = System.currentTimeMillis()
                val minute = 60_000L
                productDao.insertAll(
                    listOf(
                        ProductEntity(
                            title = "Set Alat Gambar Teknik",
                            price = 60_000,
                            categoryId = CAT_ALAT_KULIAH,
                            condition = "Baik",
                            sellerId = DEMO_USER_ID,
                            description = "Penggaris, jangka, dan alat gambar dalam pouch.",
                            createdAt = now - 4 * minute
                        ),
                        ProductEntity(
                            title = "Jas Lab Putih — Ukuran M",
                            price = 45_000,
                            categoryId = CAT_PERLENGKAPAN,
                            condition = "Sangat baik",
                            sellerId = DEMO_USER_ID,
                            description = "Jas lab dipakai satu semester, sudah dicuci dan siap pakai.",
                            createdAt = now - 3 * minute
                        ),
                        ProductEntity(
                            title = "Scientific Calculator FX-991",
                            price = 135_000,
                            categoryId = CAT_ELEKTRONIK,
                            condition = "Baik",
                            sellerId = DEMO_USER_ID,
                            description = "Kalkulator scientific berfungsi normal, termasuk cover pelindung.",
                            createdAt = now - 2 * minute
                        ),
                        ProductEntity(
                            title = "Calculus: Early Transcendentals",
                            price = 85_000,
                            categoryId = CAT_BUKU,
                            condition = "Sangat baik",
                            sellerId = DEMO_USER_ID,
                            description = "Buku kalkulus edisi 8. Beberapa bagian diberi stabilo, tetapi isi masih lengkap dan rapi.",
                            createdAt = now - minute
                        )
                    )
                )
            }
        }
    }
}
