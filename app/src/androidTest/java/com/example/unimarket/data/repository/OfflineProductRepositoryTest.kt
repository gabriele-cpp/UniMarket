package com.example.unimarket.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.unimarket.data.local.UniMarketDatabase
import com.example.unimarket.data.model.ProductInput
import com.example.unimarket.data.model.ProductSort
import com.example.unimarket.data.model.ProductStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test repository barang dengan Room in-memory: query katalog/pencarian yang sebenarnya,
 * aturan kepemilikan (hanya penjual yang boleh mengubah/menghapus), dan pembersihan foto.
 */
@RunWith(AndroidJUnit4::class)
class OfflineProductRepositoryTest {

    private lateinit var db: UniMarketDatabase
    private lateinit var images: FakeImageStorage
    private lateinit var repository: OfflineProductRepository
    private lateinit var seed: Seed

    @Before
    fun setUp() = runTest {
        db = inMemoryDatabase()
        images = FakeImageStorage()
        repository = OfflineProductRepository(db.productDao(), images)
        seed = db.seedBasics()
    }

    @After
    fun tearDown() = db.close()

    private fun input(title: String = "Kalkulator Casio", sellerId: Long = seed.sellerId, imageUri: String? = null) =
        ProductInput(
            title = title, price = 150_000, categoryId = seed.elektronikId, condition = "Baik",
            sellerId = sellerId, description = "Masih normal", imageUri = imageUri, stock = 2
        )

    @Test
    fun addProduct_savesProductWithSellerAndCategory() = runTest {
        val id = repository.addProduct(input()).toInt()

        val product = repository.getProductById(id)
        assertNotNull(product)
        assertEquals("Kalkulator Casio", product!!.title)
        assertEquals("Sari Penjual", product.sellerName)
        assertEquals("Elektronik", product.categoryName)
        assertEquals(2, product.stock)
        assertEquals(ProductStatus.TERSEDIA, product.status)
    }

    @Test
    fun addProduct_copiesPickedPhotoToInternalStorage() = runTest {
        val id = repository.addProduct(input(imageUri = "content://media/picked/7")).toInt()

        assertEquals(listOf("content://media/picked/7"), images.imported)
        assertTrue(images.isStoredImage(repository.getProductById(id)!!.imageUri))
    }

    @Test
    fun marketplace_hidesSoldProducts_andFiltersByCategory() = runTest {
        db.insertProduct(seed.sellerId, "Buku Kalkulus", categoryId = seed.bukuId)
        db.insertProduct(seed.sellerId, "Laptop Bekas", categoryId = seed.elektronikId)
        db.insertProduct(seed.sellerId, "Buku Terjual", categoryId = seed.bukuId, status = ProductStatus.TERJUAL)

        val all = repository.getProductsByCategory(null).first().map { it.title }
        val buku = repository.getProductsByCategory(seed.bukuId).first().map { it.title }

        assertEquals(setOf("Buku Kalkulus", "Laptop Bekas"), all.toSet())
        assertEquals(listOf("Buku Kalkulus"), buku)
    }

    @Test
    fun search_matchesTitleDescriptionAndCategoryName_caseInsensitive() = runTest {
        db.insertProduct(seed.sellerId, "Kalkulator", categoryId = seed.elektronikId)
        db.insertProduct(seed.sellerId, "Modul Praktikum", description = "berisi rumus kalkulus", categoryId = seed.bukuId)
        db.insertProduct(seed.sellerId, "Novel", categoryId = seed.bukuId)

        assertEquals(setOf("Kalkulator", "Modul Praktikum"), repository.searchProducts("KALKU", null, ProductSort.NEWEST).first().map { it.title }.toSet())
        assertEquals(setOf("Modul Praktikum", "Novel"), repository.searchProducts("buku", null, ProductSort.NEWEST).first().map { it.title }.toSet())
        // Spasi di awal/akhir kata kunci diabaikan.
        assertEquals(listOf("Novel"), repository.searchProducts("  novel  ", null, ProductSort.NEWEST).first().map { it.title })
        assertTrue(repository.searchProducts("tidak-ada", null, ProductSort.NEWEST).first().isEmpty())
    }

    @Test
    fun search_sortsByPrice() = runTest {
        db.insertProduct(seed.sellerId, "Sedang", price = 50_000)
        db.insertProduct(seed.sellerId, "Murah", price = 10_000)
        db.insertProduct(seed.sellerId, "Mahal", price = 90_000)

        assertEquals(listOf("Murah", "Sedang", "Mahal"), repository.searchProducts("", null, ProductSort.PRICE_ASC).first().map { it.title })
        assertEquals(listOf("Mahal", "Sedang", "Murah"), repository.searchProducts("", null, ProductSort.PRICE_DESC).first().map { it.title })
    }

    @Test
    fun productsBySeller_includesSoldProducts_andCountsByStatus() = runTest {
        db.insertProduct(seed.sellerId, "A")
        db.insertProduct(seed.sellerId, "B", status = ProductStatus.TERJUAL)
        db.insertProduct(seed.otherUserId, "Milik orang lain")

        assertEquals(setOf("A", "B"), repository.getProductsBySeller(seed.sellerId).first().map { it.title }.toSet())
        assertEquals(1, repository.countBySellerAndStatus(seed.sellerId, ProductStatus.TERJUAL).first())
    }

    @Test
    fun updateProduct_byOwner_succeeds_andReplacesOldPhoto() = runTest {
        val id = repository.addProduct(input(imageUri = "content://old")).toInt()
        val oldImage = repository.getProductById(id)!!.imageUri

        val result = repository.updateProduct(id, input(title = "Kalkulator Casio fx-991", imageUri = "content://new"))

        assertEquals(OwnershipResult.SUCCESS, result)
        val updated = repository.getProductById(id)!!
        assertEquals("Kalkulator Casio fx-991", updated.title)
        assertEquals(listOf(oldImage), images.deleted)   // foto lama dibuang
    }

    @Test
    fun updateProduct_byOtherUser_isRejected_andDataUnchanged() = runTest {
        val id = repository.addProduct(input()).toInt()

        val result = repository.updateProduct(id, input(title = "Diubah orang lain", sellerId = seed.otherUserId))

        assertEquals(OwnershipResult.NOT_OWNER, result)
        assertEquals("Kalkulator Casio", repository.getProductById(id)!!.title)
    }

    @Test
    fun updateAndDelete_missingProduct_returnsNotFound() = runTest {
        assertEquals(OwnershipResult.NOT_FOUND, repository.updateProduct(999, input()))
        assertEquals(OwnershipResult.NOT_FOUND, repository.updateStatus(999, seed.sellerId, ProductStatus.TERJUAL))
        assertEquals(OwnershipResult.NOT_FOUND, repository.deleteProduct(999, seed.sellerId))
    }

    @Test
    fun updateStatus_onlyOwnerCanMarkAsSold() = runTest {
        val id = repository.addProduct(input()).toInt()

        assertEquals(OwnershipResult.NOT_OWNER, repository.updateStatus(id, seed.buyerId, ProductStatus.TERJUAL))
        assertEquals(ProductStatus.TERSEDIA, repository.getProductById(id)!!.status)

        assertEquals(OwnershipResult.SUCCESS, repository.updateStatus(id, seed.sellerId, ProductStatus.TERJUAL))
        assertEquals(ProductStatus.TERJUAL, repository.getProductById(id)!!.status)
    }

    @Test
    fun deleteProduct_byOwner_removesProductAndPhoto() = runTest {
        val id = repository.addProduct(input(imageUri = "content://photo")).toInt()
        val storedImage = repository.getProductById(id)!!.imageUri

        assertEquals(OwnershipResult.SUCCESS, repository.deleteProduct(id, seed.sellerId))

        assertNull(repository.getProductById(id))
        assertEquals(listOf(storedImage), images.deleted)
    }

    @Test
    fun deleteProduct_byOtherUser_keepsProduct() = runTest {
        val id = repository.addProduct(input()).toInt()

        assertEquals(OwnershipResult.NOT_OWNER, repository.deleteProduct(id, seed.buyerId))
        assertNotNull(repository.getProductById(id))
        assertTrue(images.deleted.isEmpty())
    }
}
