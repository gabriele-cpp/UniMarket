package com.example.unimarket.data.repository

import android.content.Context
import android.net.Uri
import com.example.unimarket.data.local.UserPreferences
import com.example.unimarket.data.local.CategoryDao
import com.example.unimarket.data.local.ImageStorage
import com.example.unimarket.data.local.ProductDao
import com.example.unimarket.data.local.ProductEntity
import com.example.unimarket.data.local.UserDao
import com.example.unimarket.data.local.UserEntity
import com.example.unimarket.data.model.Product
import com.example.unimarket.data.model.ProductInput
import com.example.unimarket.data.model.ProductSort
import com.example.unimarket.data.model.ProductStatus
import com.example.unimarket.data.remote.SupabaseApi
import com.example.unimarket.data.remote.SupabaseProductDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.Instant
import java.util.UUID

class SupabaseProductRepository(
    private val supabaseApi: SupabaseApi,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val userDao: UserDao,
    private val imageStorage: ImageStorage,
    private val context: Context,
    private val supabaseBaseUrl: String,
    private val coroutineScope: CoroutineScope,
    private val userPreferences: UserPreferences
) : ProductRepository {

    private val offlineRepo = OfflineProductRepository(productDao, imageStorage)
    private val syncMutex = Mutex()
    private var lastSyncTimestamp = 0L

    companion object {
        private const val SYNC_COOLDOWN_MS = 30_000L // 30 detik cooldown antar sinkronisasi otomatis
    }

    init {
        // Sinkronisasi awal di background saat aplikasi dibuka
        coroutineScope.launch(Dispatchers.IO) {
            syncWithSupabase(force = true)
        }
    }

    /**
     * Sinkronisasi data dari Supabase ke database lokal Room.
     * Menggunakan throttling cooldown dan mutex untuk menghindari spam request.
     */
    suspend fun syncWithSupabase(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastSyncTimestamp < SYNC_COOLDOWN_MS)) {
            return
        }

        if (!syncMutex.tryLock()) return
        try {
            val remoteProducts = supabaseApi.getProducts()
            if (remoteProducts.isNotEmpty()) {
                val entities = remoteProducts.map { dto ->
                    val rawPrice = dto.price.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    val catId = when (dto.category) {
                        "Buku" -> 1
                        "Elektronik" -> 2
                        "Perlengkapan" -> 3
                        "Alat Kuliah" -> 4
                        else -> 1
                    }

                    // Kaitkan ke seller lokal atau buatkan record seller jika belum ada
                    val sellerName = dto.seller.trim().ifEmpty { "Penjual" }
                    val sellerId = getOrCreateSellerId(sellerName)

                    val productStatus = if (dto.status?.equals("TERJUAL", ignoreCase = true) == true) {
                        ProductStatus.TERJUAL
                    } else {
                        ProductStatus.TERSEDIA
                    }

                    ProductEntity(
                        id = dto.id?.toInt() ?: 0,
                        title = dto.title,
                        price = rawPrice,
                        categoryId = catId,
                        condition = dto.condition,
                        sellerId = sellerId,
                        description = dto.description,
                        imageUri = dto.image_uri,
                        location = dto.location,
                        deliveryMethod = dto.delivery_method,
                        status = productStatus,
                        createdAt = parseCreatedAt(dto.created_at)
                    )
                }
                productDao.upsertAll(entities)
                lastSyncTimestamp = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // Jika offline atau jaringan lambat, database lokal Room tetap berjalan mulus
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun getOrCreateSellerId(sellerName: String): Long {
        val existing = userDao.getUserByName(sellerName)
        if (existing != null) return existing.id

        val cleanEmail = "${sellerName.replace(" ", "").lowercase()}@unimarket.id"
        val existingByEmail = userDao.getUserByEmail(cleanEmail)
        if (existingByEmail != null) return existingByEmail.id

        val newSeller = UserEntity(
            name = sellerName,
            email = cleanEmail,
            passwordHash = "",
            phone = "081234567890"
        )
        return userDao.upsertUser(newSeller)
    }

    private fun parseCreatedAt(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            Instant.parse(dateStr).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    override fun getProductsByCategory(categoryId: Int?): Flow<List<Product>> {
        // Picu sinkronisasi hanya jika cooldown sudah lewat (tidak membebani jaringan di tiap tab filter)
        coroutineScope.launch(Dispatchers.IO) { syncWithSupabase(force = false) }
        return offlineRepo.getProductsByCategory(categoryId)
    }

    override fun searchProducts(
        query: String,
        categoryId: Int?,
        sort: ProductSort
    ): Flow<List<Product>> =
        offlineRepo.searchProducts(query, categoryId, sort)

    override fun getProductsBySeller(sellerId: Long): Flow<List<Product>> =
        offlineRepo.getProductsBySeller(sellerId)

    override fun countBySellerAndStatus(sellerId: Long, status: ProductStatus): Flow<Int> =
        offlineRepo.countBySellerAndStatus(sellerId, status)

    override fun observeProduct(id: Int): Flow<Product?> =
        offlineRepo.observeProduct(id)

    override suspend fun getProductById(id: Int): Product? =
        offlineRepo.getProductById(id)

    override suspend fun addProduct(input: ProductInput): Long = withContext(Dispatchers.IO) {
        var remoteImageUrl: String? = null

        // 1. Jika ada gambar, upload ke Supabase Storage (product-images bucket)
        val imageUriString = input.imageUri
        if (!imageUriString.isNullOrEmpty()) {
            try {
                val bytes = readImageBytes(imageUriString)
                if (bytes != null) {
                    val fileName = "prod_${UUID.randomUUID()}.jpg"
                    val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                    supabaseApi.uploadImage(fileName, requestBody)

                    val baseUrl = if (supabaseBaseUrl.endsWith("/")) supabaseBaseUrl else "$supabaseBaseUrl/"
                    remoteImageUrl = "${baseUrl}storage/v1/object/public/product-images/$fileName"
                    android.util.Log.d("SupabaseProduct", "Upload gambar berhasil: $remoteImageUrl")
                }
            } catch (e: Exception) {
                android.util.Log.e("SupabaseProduct", "Upload gambar gagal: ${e.message}", e)
                remoteImageUrl = null
            }
        }

        // 2. Ambil data nama kategori dan penjual
        val categoryName = when (input.categoryId) {
            1 -> "Buku"
            2 -> "Elektronik"
            3 -> "Perlengkapan"
            4 -> "Alat Kuliah"
            else -> "Lainnya"
        }
        val seller = userDao.getUserById(input.sellerId)
        val sellerName = seller?.name ?: "Penjual"

        // 3. Simpan ke Supabase PostgreSQL
        var remoteId: Long? = null
        try {
            val dto = SupabaseProductDto(
                title = input.title,
                price = "Rp${input.price}",
                category = categoryName,
                condition = input.condition,
                seller = sellerName,
                seller_id = userPreferences.getUserUuid(),
                description = input.description,
                location = input.location,
                delivery_method = input.deliveryMethod,
                image_uri = remoteImageUrl ?: input.imageUri
            )
            val insertedList = supabaseApi.insertProduct(dto)
            remoteId = insertedList.firstOrNull()?.id
            android.util.Log.d("SupabaseProduct", "Produk tersimpan di Supabase, id=$remoteId")
        } catch (e: Exception) {
            android.util.Log.e("SupabaseProduct", "Gagal menyimpan produk ke Supabase: ${e.message}", e)
        }

        // 4. Simpan ke Room lokal
        val localImage = remoteImageUrl ?: input.imageUri
        val localEntity = ProductEntity(
            id = remoteId?.toInt() ?: 0,
            title = input.title,
            price = input.price,
            categoryId = input.categoryId,
            condition = input.condition,
            sellerId = input.sellerId,
            description = input.description,
            imageUri = localImage,
            location = input.location,
            deliveryMethod = input.deliveryMethod,
            status = ProductStatus.TERSEDIA,
            createdAt = System.currentTimeMillis()
        )
        productDao.upsertProduct(localEntity)
    }

    override suspend fun updateProduct(productId: Int, input: ProductInput): OwnershipResult =
        withContext(Dispatchers.IO) {
            offlineRepo.updateProduct(productId, input)
        }

    override suspend fun updateStatus(
        productId: Int,
        requesterId: Long,
        status: ProductStatus
    ): OwnershipResult = withContext(Dispatchers.IO) {
        val result = offlineRepo.updateStatus(productId, requesterId, status)
        if (result == OwnershipResult.SUCCESS) {
            try {
                supabaseApi.updateProduct(
                    idFilter = "eq.$productId",
                    body = mapOf("status" to status.name)
                )
            } catch (e: Exception) {
                android.util.Log.e("SupabaseProduct", "Gagal update status di Supabase: ${e.message}")
            }
        }
        result
    }

    override suspend fun refresh() {
        syncWithSupabase(force = true)
    }

    override suspend fun markProductSold(productId: Int) {
        withContext(Dispatchers.IO) {
            productDao.setStatus(productId, ProductStatus.TERJUAL)
            try {
                supabaseApi.updateProduct(
                    idFilter = "eq.$productId",
                    body = mapOf("status" to ProductStatus.TERJUAL.name)
                )
            } catch (e: Exception) {
                android.util.Log.e("SupabaseProduct", "Gagal sinkron status TERJUAL ke Supabase: ${e.message}")
            }
        }
    }

    override suspend fun deleteProduct(productId: Int, requesterId: Long): OwnershipResult =
        withContext(Dispatchers.IO) {
            try {
                supabaseApi.deleteProduct("eq.$productId")
            } catch (e: Exception) {
                // Lanjutkan penghapusan lokal
            }
            offlineRepo.deleteProduct(productId, requesterId)
        }

    private fun readImageBytes(uriString: String): ByteArray? {
        return try {
            if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                context.contentResolver.openInputStream(Uri.parse(uriString))?.use { it.readBytes() }
            } else {
                val file = File(uriString)
                if (file.exists()) file.readBytes() else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
