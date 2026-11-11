package com.example.unimarket.data.local

import androidx.room.TypeConverter
import com.example.unimarket.data.model.ProductStatus

class Converters {
    @TypeConverter
    fun fromProductStatus(status: ProductStatus): String = status.name

    @TypeConverter
    fun toProductStatus(value: String): ProductStatus =
        ProductStatus.entries.firstOrNull { it.name == value } ?: ProductStatus.TERSEDIA
}
