package com.example.unimarket.data.repository

import com.example.unimarket.data.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>

    suspend fun getCategoryById(id: Int): Category?
}
