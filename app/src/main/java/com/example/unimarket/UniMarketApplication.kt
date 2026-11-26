package com.example.unimarket

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.unimarket.data.local.DatabaseSeeder
import com.example.unimarket.data.local.UniMarketDatabase
import com.example.unimarket.di.ApplicationScope
import com.example.unimarket.ui.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Dependency disediakan Hilt, lihat di/AppModule.kt. */
@HiltAndroidApp
class UniMarketApplication : Application(), ImageLoaderFactory {

    @Inject lateinit var database: UniMarketDatabase

    /** Scope selama aplikasi hidup (disediakan Hilt), dipakai untuk seeding database. */
    @Inject @field:ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()  // Hilt mengisi field @Inject di sini
        NotificationHelper.initChannel(this)
        applicationScope.launch {
            DatabaseSeeder.seedIfEmpty(database)
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
