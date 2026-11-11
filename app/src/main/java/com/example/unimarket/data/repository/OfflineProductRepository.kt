package com.example.unimarket.data.repository

import com.example.unimarket.data.local.ImageStorage
import com.example.unimarket.data.local.ProductDao
import com.example.unimarket.data.local.ProductWithSeller
import com.example.unimarket.data.local.toEntity
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.ProductInput
import com.example.unimarket.data.model.ProductSort
import com.example.unimarket.data.model.ProductStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineProductRepository(
    private val productDao: ProductDao,
    private val imageStorage: ImageStorage
) : ProductRepository {

    override fun getProductsByCategory(categoryId: Int?): Flow<List<Product>> =
        productDao.getMarketplaceProducts(categoryId, ProductStatus.TERJUAL).map { it.toProducts() }

    override fun searchProducts(query: String, categoryId: Int?, sort: ProductSort): Flow<List<Product>> =
        productDao.searchProducts(query.trim(), categoryId, ProductStatus.TERJUAL, sort.name)
            .map { it.toProducts() }

    override fun getProductsBySeller(sellerId: Long): Flow<List<Product>> =
        productDao.getProductsBySeller(sellerId).map { it.toProducts() }

    override fun countBySellerAndStatus(sellerId: Long, status: ProductStatus): Flow<Int> =
        productDao.countBySellerAndStatus(sellerId, status)

    override fun observeProduct(id: Int): Flow<Product?> =
        productDao.observeProductById(id).map { it?.toProduct() }

    override suspend fun getProductById(id: Int): Product? =
        productDao.getProductById(id)?.toProduct()

    override suspend fun addProduct(input: ProductInput): Long {
        val storedImage = input.imageUri?.let { prepareImage(it) }
        return try {
            productDao.insertProduct(input.copy(imageUri = storedImage).toEntity())
        } catch (e: Exception) {
            // Insert gagal: buang foto yang baru disalin agar tidak jadi file yatim.
            if (storedImage != input.imageUri) imageStorage.deleteImage(storedImage)
            throw e
        }
    }

    override suspend fun updateProduct(productId: Int, input: ProductInput): OwnershipResult {
        val existing = productDao.getProductEntityById(productId) ?: return OwnershipResult.NOT_FOUND
        if (existing.sellerId != input.sellerId) return OwnershipResult.NOT_OWNER

        val newImage = when (input.imageUri) {
            existing.imageUri -> existing.imageUri          // foto tidak diganti
            null -> null                                    // foto dihapus
            else -> prepareImage(input.imageUri)            // foto baru dari picker
        }
        productDao.updateProduct(
            existing.copy(
                title = input.title,
                price = input.price,
                categoryId = input.categoryId,
                condition = input.condition,
                description = input.description,
                imageUri = newImage,
                location = input.location,
                deliveryMethod = input.deliveryMethod
                // id, sellerId, status, createdAt tetap
            )
        )
        if (existing.imageUri != newImage) imageStorage.deleteImage(existing.imageUri)
        return OwnershipResult.SUCCESS
    }

    override suspend fun updateStatus(productId: Int, requesterId: Long, status: ProductStatus): OwnershipResult {
        val existing = productDao.getProductEntityById(productId) ?: return OwnershipResult.NOT_FOUND
        if (existing.sellerId != requesterId) return OwnershipResult.NOT_OWNER
        productDao.updateStatus(productId, requesterId, status)
        return OwnershipResult.SUCCESS
    }

    override suspend fun deleteProduct(productId: Int, requesterId: Long): OwnershipResult {
        val existing = productDao.getProductEntityById(productId) ?: return OwnershipResult.NOT_FOUND
        if (existing.sellerId != requesterId) return OwnershipResult.NOT_OWNER
        productDao.deleteProduct(existing)
        imageStorage.deleteImage(existing.imageUri)
        return OwnershipResult.SUCCESS
    }

    /** Foto dari picker disalin ke penyimpanan internal; yang sudah tersimpan dipakai apa adanya. */
    private suspend fun prepareImage(uri: String): String =
        if (imageStorage.isStoredImage(uri)) uri else imageStorage.importImage(uri)

    private fun List<ProductWithSeller>.toProducts(): List<Product> = map { it.toProduct() }
}
