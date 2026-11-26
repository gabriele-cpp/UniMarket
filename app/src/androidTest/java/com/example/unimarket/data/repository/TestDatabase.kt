package com.example.unimarket.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.unimarket.data.local.CategoryEntity
import com.example.unimarket.data.local.ImageStorage
import com.example.unimarket.data.local.ProductEntity
import com.example.unimarket.data.local.UniMarketDatabase
import com.example.unimarket.data.local.UserEntity

/** Context aplikasi yang sedang dites. */
val targetContext: Context
    get() = InstrumentationRegistry.getInstrumentation().targetContext

/** Database Room di memori: skema & query asli, tetapi hilang setelah tes selesai. */
fun inMemoryDatabase(): UniMarketDatabase =
    Room.inMemoryDatabaseBuilder(targetContext, UniMarketDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/** Data dasar yang dipakai beberapa tes: 2 kategori, penjual, dan pembeli. */
class Seed(val sellerId: Long, val buyerId: Long, val otherUserId: Long, val bukuId: Int, val elektronikId: Int)

suspend fun UniMarketDatabase.seedBasics(): Seed {
    categoryDao().insertAll(listOf(CategoryEntity(id = 1, name = "Buku"), CategoryEntity(id = 2, name = "Elektronik")))
    val seller = userDao().insertUser(user("Sari Penjual", "sari@kampus.ac.id"))
    val buyer = userDao().insertUser(user("Bima Pembeli", "bima@kampus.ac.id"))
    val other = userDao().insertUser(user("Orang Lain", "lain@kampus.ac.id"))
    return Seed(seller, buyer, other, bukuId = 1, elektronikId = 2)
}

suspend fun UniMarketDatabase.insertProduct(
    sellerId: Long,
    title: String,
    price: Long = 50_000,
    categoryId: Int = 1,
    description: String = "",
    imageUri: String? = null,
    status: com.example.unimarket.data.model.ProductStatus = com.example.unimarket.data.model.ProductStatus.TERSEDIA,
    createdAt: Long = System.currentTimeMillis()
): Int = productDao().insertProduct(
    ProductEntity(
        title = title, price = price, categoryId = categoryId, condition = "Baik",
        sellerId = sellerId, description = description, imageUri = imageUri,
        status = status, createdAt = createdAt
    )
).toInt()

private fun user(name: String, email: String) =
    UserEntity(name = name, email = email, passwordHash = "x", phone = "6281200000000")

/** ImageStorage palsu: mencatat foto yang disalin & dihapus tanpa menyentuh file sungguhan. */
class FakeImageStorage : ImageStorage {
    val imported = mutableListOf<String>()
    val deleted = mutableListOf<String?>()

    override suspend fun importImage(sourceUri: String): String =
        "$STORED_PREFIX${imported.size}.jpg".also { imported += sourceUri }

    override suspend fun deleteImage(path: String?) {
        if (path != null) deleted += path
    }

    override fun isStoredImage(path: String?): Boolean = path?.startsWith(STORED_PREFIX) == true

    companion object {
        const val STORED_PREFIX = "/internal/images/"
    }
}
