package com.example.unimarket.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.unimarket.data.model.ProductDefaults
import com.example.unimarket.data.model.ProductInput
import com.example.unimarket.data.model.ProductStatus

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["sellerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["sellerId"]), Index(value = ["categoryId"])]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    /** Harga dalam Rupiah tanpa format, contoh 85000. */
    val price: Long,
    val categoryId: Int,
    val condition: String,
    val sellerId: Long,
    val description: String,
    val imageUri: String? = null,
    val location: String = ProductDefaults.LOCATION,
    val deliveryMethod: String = ProductDefaults.DELIVERY_METHOD,
    val status: ProductStatus = ProductStatus.TERSEDIA,
    val createdAt: Long = System.currentTimeMillis()
)

fun ProductInput.toEntity(): ProductEntity = ProductEntity(
    title = title,
    price = price,
    categoryId = categoryId,
    condition = condition,
    sellerId = sellerId,
    description = description,
    imageUri = imageUri,
    location = location,
    deliveryMethod = deliveryMethod
)
