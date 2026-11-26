package com.example.unimarket.di

import android.content.Context
import com.example.unimarket.BuildConfig
import com.example.unimarket.data.local.CategoryDao
import com.example.unimarket.data.local.InternalImageStorage
import com.example.unimarket.data.local.MessageDao
import com.example.unimarket.data.local.OrderDao
import com.example.unimarket.data.local.ProductDao
import com.example.unimarket.data.local.UniMarketDatabase
import com.example.unimarket.data.local.UserDao
import com.example.unimarket.data.local.UserPreferences
import com.example.unimarket.data.remote.PaymentApi
import com.example.unimarket.data.remote.SupabaseApi
import com.example.unimarket.data.remote.SupabaseRealtime
import com.example.unimarket.data.repository.CategoryRepository
import com.example.unimarket.data.repository.DefaultSessionRepository
import com.example.unimarket.data.repository.MessageRepository
import com.example.unimarket.data.repository.OfflineCategoryRepository
import com.example.unimarket.data.repository.OnlinePaymentRepository
import com.example.unimarket.data.repository.PaymentRepository
import com.example.unimarket.data.repository.ProductRepository
import com.example.unimarket.data.repository.SessionRepository
import com.example.unimarket.data.repository.SupabaseMessageRepository
import com.example.unimarket.data.repository.SupabaseProductRepository
import com.example.unimarket.data.repository.SupabaseUserRepository
import com.example.unimarket.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Scope selama aplikasi hidup, untuk pekerjaan latar (seeding, sinkronisasi Supabase). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** Sumber data: database Room, preferensi, dan klien jaringan. */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences =
        UserPreferences(context)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UniMarketDatabase =
        UniMarketDatabase.getDatabase(context)

    @Provides fun provideUserDao(db: UniMarketDatabase): UserDao = db.userDao()
    @Provides fun provideProductDao(db: UniMarketDatabase): ProductDao = db.productDao()
    @Provides fun provideCategoryDao(db: UniMarketDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideMessageDao(db: UniMarketDatabase): MessageDao = db.messageDao()
    @Provides fun provideOrderDao(db: UniMarketDatabase): OrderDao = db.orderDao()

    @Provides
    @Singleton
    fun provideSupabaseApi(userPreferences: UserPreferences): SupabaseApi =
        SupabaseApi.create(
            baseUrl = BuildConfig.SUPABASE_URL,
            apiKey = BuildConfig.SUPABASE_KEY,
            tokenProvider = { userPreferences.getAccessToken() }
        )

    @Provides
    @Singleton
    fun provideSupabaseRealtime(): SupabaseRealtime =
        SupabaseRealtime(baseUrl = BuildConfig.SUPABASE_URL, apiKey = BuildConfig.SUPABASE_KEY)

    /** Alamat payment-server dari local.properties (PAYMENT_BASE_URL), lihat app/build.gradle.kts. */
    @Provides
    @Singleton
    fun providePaymentApi(): PaymentApi = PaymentApi.create(BuildConfig.PAYMENT_BASE_URL)
}

/**
 * Implementasi repository yang dipakai aplikasi. ViewModel hanya bergantung pada interface,
 * sehingga unit test cukup memberi Fake repository lewat constructor.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        supabaseApi: SupabaseApi,
        userDao: UserDao,
        userPreferences: UserPreferences
    ): UserRepository = SupabaseUserRepository(
        supabaseApi = supabaseApi,
        userDao = userDao,
        userPreferences = userPreferences
    )

    @Provides
    @Singleton
    fun provideCategoryRepository(categoryDao: CategoryDao): CategoryRepository =
        OfflineCategoryRepository(categoryDao)

    @Provides
    @Singleton
    fun provideProductRepository(
        @ApplicationContext context: Context,
        supabaseApi: SupabaseApi,
        productDao: ProductDao,
        categoryDao: CategoryDao,
        userDao: UserDao,
        userPreferences: UserPreferences,
        @ApplicationScope scope: CoroutineScope
    ): ProductRepository = SupabaseProductRepository(
        supabaseApi = supabaseApi,
        productDao = productDao,
        categoryDao = categoryDao,
        userDao = userDao,
        imageStorage = InternalImageStorage(context),
        context = context,
        supabaseBaseUrl = BuildConfig.SUPABASE_URL,
        coroutineScope = scope,
        userPreferences = userPreferences
    )

    @Provides
    @Singleton
    fun provideMessageRepository(
        @ApplicationContext context: Context,
        supabaseApi: SupabaseApi,
        messageDao: MessageDao,
        productDao: ProductDao,
        userDao: UserDao,
        userPreferences: UserPreferences,
        realtime: SupabaseRealtime,
        @ApplicationScope scope: CoroutineScope
    ): MessageRepository = SupabaseMessageRepository(
        supabaseApi = supabaseApi,
        messageDao = messageDao,
        productDao = productDao,
        userDao = userDao,
        coroutineScope = scope,
        context = context,
        userPreferences = userPreferences,
        realtime = realtime
    )

    @Provides
    @Singleton
    fun providePaymentRepository(
        orderDao: OrderDao,
        productDao: ProductDao,
        userDao: UserDao,
        api: PaymentApi,
        productRepository: ProductRepository
    ): PaymentRepository = OnlinePaymentRepository(
        orderDao = orderDao,
        productDao = productDao,
        userDao = userDao,
        api = api,
        productRepository = productRepository
    )

    @Provides
    @Singleton
    fun provideSessionRepository(
        userPreferences: UserPreferences,
        userRepository: UserRepository
    ): SessionRepository = DefaultSessionRepository(userPreferences, userRepository)
}
