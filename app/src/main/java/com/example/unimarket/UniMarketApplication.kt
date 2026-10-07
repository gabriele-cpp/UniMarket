package com.example.unimarket

import android.app.Application
import com.example.unimarket.data.local.UniMarketDatabase
import com.example.unimarket.data.repository.ProductRepository

class UniMarketApplication : Application() {
    
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppDataContainer(this)
    }
}

interface AppContainer {
    val productRepository: ProductRepository
    val userPreferences: com.example.unimarket.data.local.UserPreferences
}

class AppDataContainer(private val application: Application) : AppContainer {
    override val productRepository: ProductRepository by lazy {
        ProductRepository(UniMarketDatabase.getDatabase(application).productDao())
    }
    
    override val userPreferences: com.example.unimarket.data.local.UserPreferences by lazy {
        com.example.unimarket.data.local.UserPreferences(application)
    }
}
