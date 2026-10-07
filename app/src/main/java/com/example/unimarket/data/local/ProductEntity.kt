package com.example.unimarket.data.local

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.unimarket.data.model.Product

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val price: String,
    val category: String,
    val condition: String,
    val seller: String,
    val description: String,
    val mark: String,
    val color: Int, // Room stores it as Int
    val imageUri: String? = null,
    val location: String = "Area Kampus",
    val deliveryMethod: String = "COD Kampus"
) {
    fun toProduct(): Product = Product(
        id = id,
        title = title,
        price = price,
        category = category,
        condition = condition,
        seller = seller,
        description = description,
        mark = mark,
        color = Color(color),
        imageUri = imageUri,
        location = location,
        deliveryMethod = deliveryMethod
    )
}

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    title = title,
    price = price,
    category = category,
    condition = condition,
    seller = seller,
    description = description,
    mark = mark,
    color = color.toArgb(),
    imageUri = imageUri,
    location = location,
    deliveryMethod = deliveryMethod
)
