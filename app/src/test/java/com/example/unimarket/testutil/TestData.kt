package com.example.unimarket.testutil

import com.example.unimarket.data.local.UserEntity
import com.example.unimarket.data.model.Category
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.ProductStatus

/**
 * Pembuat data uji dengan nilai default.
 *
 * Test cukup menulis bagian yang PENTING bagi skenarionya:
 *     product(title = "Kalkulator", price = 150_000)
 * bukan konstruktor lengkap dengan 15 parameter. Jika model bertambah field,
 * cukup ubah satu tempat ini, bukan puluhan test.
 */

val BUKU = Category(id = 1, name = "Buku")
val ELEKTRONIK = Category(id = 2, name = "Elektronik")
val PAKAIAN = Category(id = 3, name = "Pakaian")
val TEST_CATEGORIES = listOf(BUKU, ELEKTRONIK, PAKAIAN)

fun category(id: Int = 1, name: String = "Kategori $id") = Category(id = id, name = name)

fun product(
    title: String = "Barang",
    id: Int = title.hashCode() and Int.MAX_VALUE,   // id unik per judul, cukup untuk test
    price: Long = 50_000,
    category: Category = BUKU,
    sellerId: Long = 1L,
    sellerName: String = "Penjual $sellerId",
    description: String = "",
    status: ProductStatus = ProductStatus.TERSEDIA,
    createdAt: Long = 0L,
) = Product(
    id = id,
    title = title,
    price = price,
    categoryId = category.id,
    categoryName = category.name,
    condition = "Baik",
    sellerId = sellerId,
    sellerName = sellerName,
    sellerPhone = "6281234567890",
    description = description,
    status = status,
    createdAt = createdAt,
)

fun user(
    id: Long = 1L,
    name: String = "Pengguna $id",
    email: String = "user$id@kampus.ac.id",
    phone: String = "6281234567890",
) = UserEntity(id = id, name = name, email = email, passwordHash = "", phone = phone, createdAt = 0L)
