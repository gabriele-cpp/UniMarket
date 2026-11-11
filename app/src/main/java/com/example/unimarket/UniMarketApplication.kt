package com.example.unimarket

import android.app.Application
import com.example.unimarket.data.local.DatabaseSeeder
import com.example.unimarket.data.local.InternalImageStorage
import com.example.unimarket.data.local.UniMarketDatabase
import com.example.unimarket.data.local.UserPreferences
import com.example.unimarket.data.repository.CategoryRepository
import com.example.unimarket.data.repository.DefaultSessionRepository
import com.example.unimarket.data.repository.MessageRepository
import com.example.unimarket.data.repository.OfflineMessageRepository
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.data.repository.OfflineCategoryRepository
import com.example.unimarket.data.repository.OfflineProductRepository
import com.example.unimarket.data.repository.OfflineUserRepository
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class UniMarketApplication : Application() {

    lateinit var container: AppContainer

    /** Scope selama aplikasi hidup, dipakai untuk pekerjaan latar seperti seeding database. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val dataContainer = AppDataContainer(this)
        container = dataContainer
        applicationScope.launch {
            dataContainer.seedDatabaseIfEmpty()
        }
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
}

class AppDataContainer(private val application: Application) : AppContainer {

    private val database: UniMarketDatabase by lazy {
        UniMarketDatabase.getDatabase(application)
    }

    override val userRepository: UserRepository by lazy {
        OfflineUserRepository(database.userDao())
    }

    override val categoryRepository: CategoryRepository by lazy {
        OfflineCategoryRepository(database.categoryDao())
    }

    override val productRepository: ProductRepository by lazy {
        OfflineProductRepository(database.productDao(), InternalImageStorage(application))
    }

    override val messageRepository: MessageRepository by lazy {
        OfflineMessageRepository(database.messageDao(), database.productDao())
    }

    override val sessionRepository: SessionRepository by lazy {
        DefaultSessionRepository(UserPreferences(application), userRepository)
    }

    suspend fun seedDatabaseIfEmpty() {
        DatabaseSeeder.seedIfEmpty(database)
    }
}
