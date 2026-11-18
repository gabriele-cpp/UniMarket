package com.example.unimarket.testutil

import com.example.unimarket.data.model.Category
import com.example.unimarket.data.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/** Fake untuk [CategoryRepository] (asli: OfflineCategoryRepository + Room). */
class FakeCategoryRepository(initial: List<Category> = TEST_CATEGORIES) : CategoryRepository {

    private val categories = MutableStateFlow(initial)

    /** Jika diisi, getAllCategories() gagal. */
    var observeError: Exception? = null

    /** Mengganti isi kategori di tengah test (mis. kategori dihapus). */
    fun setCategories(list: List<Category>) {
        categories.value = list
    }

    override fun getAllCategories(): Flow<List<Category>> {
        observeError?.let { e -> return flow { throw e } }
        return categories
    }

    override suspend fun getCategoryById(id: Int): Category? {
        observeError?.let { throw it }
        return categories.value.firstOrNull { it.id == id }
    }
}
