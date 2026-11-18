package com.example.unimarket.testutil

import com.example.unimarket.data.model.Category
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.ProductInput
import com.example.unimarket.data.model.ProductSort
import com.example.unimarket.data.model.ProductStatus
import com.example.unimarket.data.repository.OwnershipResult
import com.example.unimarket.data.repository.ProductRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * POLA A — fake untuk [ProductRepository] (asli: OfflineProductRepository + Room).
 *
 * Data disimpan di MutableStateFlow dalam memori. Aturan filter & urutan meniru
 * query di ProductDao (katalog publik tanpa TERJUAL, LIKE pada judul/deskripsi/
 * nama kategori, urutan terbaru) supaya ViewModel menerima data yang realistis.
 *
 * Saklar:
 *  - [observeError]   : semua fungsi Flow langsung gagal (DB rusak, migrasi gagal)
 *  - [writeError]     : semua operasi tulis melempar exception
 *  - [writeDelayMillis]: operasi tulis lambat, untuk menguji state loading/saving
 *
 * Setiap test membuat fake baru (FIRST: Independent); jangan simpan di companion.
 */
class FakeProductRepository(
    initial: List<Product> = emptyList(),
    private val categories: List<Category> = TEST_CATEGORIES,
) : ProductRepository {

    private val products = MutableStateFlow(initial)

    var observeError: Exception? = null
    var writeError: Exception? = null
    var writeDelayMillis: Long = 0

    /** Isi "database" saat ini, untuk assertion state-based. */
    val current: List<Product> get() = products.value

    // ── baca (Flow) ──────────────────────────────────────────────────────────

    override fun getProductsByCategory(categoryId: Int?): Flow<List<Product>> = observe { list ->
        list.filter { it.status != ProductStatus.TERJUAL }
            .filter { categoryId == null || it.categoryId == categoryId }
            .newestFirst()
    }

    override fun searchProducts(query: String, categoryId: Int?, sort: ProductSort): Flow<List<Product>> =
        observe { list ->
            val q = query.trim()
            val filtered = list
                .filter { it.status != ProductStatus.TERJUAL }
                .filter { categoryId == null || it.categoryId == categoryId }
                .filter {
                    q.isEmpty() ||
                        it.title.contains(q, ignoreCase = true) ||
                        it.description.contains(q, ignoreCase = true) ||
                        it.categoryName.contains(q, ignoreCase = true)
                }
                .newestFirst()
            when (sort) {
                ProductSort.NEWEST -> filtered
                ProductSort.PRICE_ASC -> filtered.sortedBy { it.price }          // stabil: seri tetap terbaru dulu
                ProductSort.PRICE_DESC -> filtered.sortedByDescending { it.price }
            }
        }

    override fun getProductsBySeller(sellerId: Long): Flow<List<Product>> = observe { list ->
        list.filter { it.sellerId == sellerId }.newestFirst()
    }

    override fun countBySellerAndStatus(sellerId: Long, status: ProductStatus): Flow<Int> = observe { list ->
        list.count { it.sellerId == sellerId && it.status == status }
    }

    override fun observeProduct(id: Int): Flow<Product?> = observe { list -> list.firstOrNull { it.id == id } }

    override suspend fun getProductById(id: Int): Product? {
        observeError?.let { throw it }
        return products.value.firstOrNull { it.id == id }
    }

    // ── tulis ────────────────────────────────────────────────────────────────

    override suspend fun addProduct(input: ProductInput): Long = write {
        val newId = (products.value.maxOfOrNull { it.id } ?: 0) + 1
        val category = categories.firstOrNull { it.id == input.categoryId }
        products.update {
            it + Product(
                id = newId,
                title = input.title,
                price = input.price,
                categoryId = input.categoryId,
                categoryName = category?.name.orEmpty(),
                condition = input.condition,
                sellerId = input.sellerId,
                sellerName = "Penjual ${input.sellerId}",
                sellerPhone = "",
                description = input.description,
                imageUri = input.imageUri,
                location = input.location,
                deliveryMethod = input.deliveryMethod,
                createdAt = (products.value.maxOfOrNull { p -> p.createdAt } ?: 0L) + 1,
            )
        }
        newId.toLong()
    }

    override suspend fun updateProduct(productId: Int, input: ProductInput): OwnershipResult = write {
        ownership(productId, input.sellerId).also { result ->
            if (result == OwnershipResult.SUCCESS) modify(productId) {
                it.copy(
                    title = input.title,
                    price = input.price,
                    categoryId = input.categoryId,
                    categoryName = categories.firstOrNull { c -> c.id == input.categoryId }?.name.orEmpty(),
                    condition = input.condition,
                    description = input.description,
                    imageUri = input.imageUri,
                    location = input.location,
                    deliveryMethod = input.deliveryMethod,
                )
            }
        }
    }

    override suspend fun updateStatus(productId: Int, requesterId: Long, status: ProductStatus): OwnershipResult =
        write {
            ownership(productId, requesterId).also { result ->
                if (result == OwnershipResult.SUCCESS) modify(productId) { it.copy(status = status) }
            }
        }

    override suspend fun deleteProduct(productId: Int, requesterId: Long): OwnershipResult = write {
        ownership(productId, requesterId).also { result ->
            if (result == OwnershipResult.SUCCESS) products.update { list -> list.filterNot { it.id == productId } }
        }
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private fun <T> observe(transform: (List<Product>) -> T): Flow<T> {
        observeError?.let { e -> return flow { throw e } }
        return products.map(transform)
    }

    private suspend fun <T> write(block: () -> T): T {
        if (writeDelayMillis > 0) delay(writeDelayMillis)   // jam virtual di runTest
        writeError?.let { throw it }
        return block()
    }

    private fun ownership(productId: Int, requesterId: Long): OwnershipResult {
        val existing = products.value.firstOrNull { it.id == productId } ?: return OwnershipResult.NOT_FOUND
        return if (existing.sellerId == requesterId) OwnershipResult.SUCCESS else OwnershipResult.NOT_OWNER
    }

    private fun modify(productId: Int, change: (Product) -> Product) {
        products.update { list -> list.map { if (it.id == productId) change(it) else it } }
    }

    private fun List<Product>.newestFirst() =
        sortedWith(compareByDescending<Product> { it.createdAt }.thenByDescending { it.id })
}
