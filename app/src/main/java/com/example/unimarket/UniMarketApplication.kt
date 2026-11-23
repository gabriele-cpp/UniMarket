package com.example.unimarket

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.unimarket.data.local.DatabaseSeeder
import com.example.unimarket.data.local.InternalImageStorage
import com.example.unimarket.data.local.UniMarketDatabase
import com.example.unimarket.data.local.UserPreferences
import com.example.unimarket.data.remote.PaymentApi
import com.example.unimarket.data.remote.SupabaseApi
import com.example.unimarket.data.repository.CategoryRepository
import com.example.unimarket.data.repository.DefaultSessionRepository
import com.example.unimarket.data.repository.MessageRepository
import com.example.unimarket.data.repository.OfflineCategoryRepository
import com.example.unimarket.data.repository.OfflineMessageRepository
import com.example.unimarket.data.repository.OnlinePaymentRepository
import com.example.unimarket.data.repository.PaymentRepository
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.data.repository.SupabaseMessageRepository
import com.example.unimarket.data.repository.SupabaseProductRepository
import com.example.unimarket.data.repository.SupabaseUserRepository
import com.example.unimarket.data.repository.UserRepository
import com.example.unimarket.ui.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UniMarketApplication : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer

    /** Scope selama aplikasi hidup, dipakai untuk pekerjaan latar seperti seeding database. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.initChannel(this)
        val dataContainer = AppDataContainer(this, applicationScope)
        container = dataContainer
        applicationScope.launch {
            dataContainer.seedDatabaseIfEmpty()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

/**
 * Manual dependency injection. Untuk testing bisa dibuat container lain yang memakai Fake repository.
 */
interface AppContainer {
    val userRepository: UserRepository
    val categoryRepository: CategoryRepository
    val productRepository: ProductRepository
    val sessionRepository: SessionRepository
    val messageRepository: MessageRepository
    val paymentRepository: PaymentRepository
}

class AppDataContainer(
    private val application: Application,
    private val scope: CoroutineScope
) : AppContainer {

    private val userPreferences: UserPreferences by lazy {
        UserPreferences(application)
    }

    private val database: UniMarketDatabase by lazy {
        UniMarketDatabase.getDatabase(application)
    }

    val supabaseApi: SupabaseApi by lazy {
        SupabaseApi.create(
            baseUrl = BuildConfig.SUPABASE_URL,
            apiKey = BuildConfig.SUPABASE_KEY,
            tokenProvider = { userPreferences.getAccessToken() }
        )
    }

    override val userRepository: UserRepository by lazy {
        SupabaseUserRepository(
            supabaseApi = supabaseApi,
            userDao = database.userDao(),
            userPreferences = userPreferences
        )
    }

    override val categoryRepository: CategoryRepository by lazy {
        OfflineCategoryRepository(database.categoryDao())
    }

    override val productRepository: ProductRepository by lazy {
        SupabaseProductRepository(
            supabaseApi = supabaseApi,
            productDao = database.productDao(),
            categoryDao = database.categoryDao(),
            userDao = database.userDao(),
            imageStorage = InternalImageStorage(application),
            context = application,
            supabaseBaseUrl = BuildConfig.SUPABASE_URL,
            coroutineScope = scope,
            userPreferences = userPreferences
        )
    }

    override val messageRepository: MessageRepository by lazy {
        SupabaseMessageRepository(
            supabaseApi = supabaseApi,
            messageDao = database.messageDao(),
            productDao = database.productDao(),
            userDao = database.userDao(),
            coroutineScope = scope,
            context = application,
            userPreferences = userPreferences
        )
    }

    override val paymentRepository: PaymentRepository by lazy {
        OnlinePaymentRepository(
            orderDao = database.orderDao(),
            productDao = database.productDao(),
            userDao = database.userDao(),
            // Alamat payment-server dari local.properties (PAYMENT_BASE_URL), lihat app/build.gradle.kts
            api = PaymentApi.create(BuildConfig.PAYMENT_BASE_URL),
            productRepository = productRepository
        )
    }

    override val sessionRepository: SessionRepository by lazy {
        DefaultSessionRepository(userPreferences, userRepository)
    }

    suspend fun seedDatabaseIfEmpty() {
        DatabaseSeeder.seedIfEmpty(database)
    }
}
