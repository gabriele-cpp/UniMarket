package com.example.unimarket.data.model

import androidx.compose.ui.graphics.Color

data class Product(
    val id: Int,
    val title: String,
    val price: String,
    val category: String,
    val condition: String,
    val seller: String,
    val description: String,
    val mark: String,
    val color: Color,
    val imageUri: String? = null,
    val location: String = "Area Kampus",
    val deliveryMethod: String = "COD Kampus & GoSend/GrabExpress"
)
