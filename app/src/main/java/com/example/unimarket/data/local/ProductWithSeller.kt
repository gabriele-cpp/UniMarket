package com.example.unimarket.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.example.unimarket.data.model.Product

/**
 * Hasil query barang beserta data penjual dan kategorinya.
 * Relation dibuat nullable supaya aman; normalnya selalu terisi karena dijaga Foreign Key.
 */
data class ProductWithSeller(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "sellerId", entityColumn = "id")
    val seller: UserEntity?,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity?
) {
    fun toProduct(): Product = Product(
        id = product.id,
        title = product.title,
        price = product.price,
        categoryId = product.categoryId,
        categoryName = category?.name ?: "Lainnya",
        condition = product.condition,
        sellerId = product.sellerId,
        sellerName = seller?.name ?: "Penjual tidak diketahui",
        sellerPhone = seller?.phone.orEmpty(),
        description = product.description,
        imageUri = product.imageUri,
        location = product.location,
        deliveryMethod = product.deliveryMethod,
        status = product.status,
        createdAt = product.createdAt
    )
}
