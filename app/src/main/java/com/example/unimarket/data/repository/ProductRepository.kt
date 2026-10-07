package com.example.unimarket.data.repository

import androidx.compose.ui.graphics.Color
import com.example.unimarket.data.local.ProductDao
import com.example.unimarket.data.local.toEntity
import com.example.unimarket.data.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class ProductRepository(private val productDao: ProductDao) {

    fun getAllProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { entities -> 
            entities.map { it.toProduct() } 
        }
    }

    fun getProductsByCategory(category: String): Flow<List<Product>> {
        if (category == "Semua") return getAllProducts()
        return productDao.getProductsByCategory(category).map { entities -> 
            entities.map { it.toProduct() } 
        }
    }

    fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.searchProducts(query).map { entities -> 
            entities.map { it.toProduct() } 
        }
    }

    suspend fun getProductById(id: Int): Product? {
        return productDao.getProductById(id)?.toProduct()
    }
    
    suspend fun addProduct(product: Product) {
        productDao.insertProduct(product.toEntity())
    }

    suspend fun populateInitialDataIfNeeded() {
        val currentList = productDao.getAllProducts().firstOrNull() ?: emptyList()
        if (currentList.isEmpty()) {
            val initialProducts = listOf(
                Product(id = 0, title = "Calculus: Early Transcendentals", price = "Rp85.000", category = "Buku", condition = "Sangat baik", seller = "Nadia Putri", description = "Buku kalkulus edisi 8. Beberapa bagian diberi stabilo, tetapi isi masih lengkap dan rapi.", mark = "BK", color = Color(0xFFD7E7F5)),
                Product(id = 0, title = "Scientific Calculator FX-991", price = "Rp135.000", category = "Elektronik", condition = "Baik", seller = "Raka Wijaya", description = "Kalkulator scientific berfungsi normal, termasuk cover pelindung.", mark = "FX", color = Color(0xFFE9DFB9)),
                Product(id = 0, title = "Jas Lab Putih \u2014 Ukuran M", price = "Rp45.000", category = "Perlengkapan", condition = "Sangat baik", seller = "Citra Lestari", description = "Jas lab dipakai satu semester, sudah dicuci dan siap pakai.", mark = "JL", color = Color(0xFFE6E1ED)),
                Product(id = 0, title = "Set Alat Gambar Teknik", price = "Rp60.000", category = "Alat Kuliah", condition = "Baik", seller = "Dimas Pratama", description = "Penggaris, jangka, dan alat gambar dalam pouch.", mark = "AT", color = Color(0xFFD9EAD8)),
            )
            productDao.insertAll(initialProducts.map { it.toEntity() })
        }
    }

    companion object {
        fun getCategories(): List<String> = listOf("Semua", "Buku", "Elektronik", "Perlengkapan", "Alat Kuliah")
        fun getPopularCategories(): List<String> = listOf("Buku", "Elektronik", "Alat Kuliah")
    }
}
