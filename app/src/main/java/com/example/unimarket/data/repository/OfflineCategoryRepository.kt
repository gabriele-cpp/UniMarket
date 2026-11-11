package com.example.unimarket.data.repository

import com.example.unimarket.data.local.CategoryDao
import com.example.unimarket.data.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineCategoryRepository(private val categoryDao: CategoryDao) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { list -> list.map { it.toCategory() } }

    override suspend fun getCategoryById(id: Int): Category? =
        categoryDao.getCategoryById(id)?.toCategory()
}
