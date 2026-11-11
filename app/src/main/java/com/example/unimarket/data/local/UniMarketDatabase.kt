package com.example.unimarket.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [UserEntity::class, CategoryEntity::class, ProductEntity::class, MessageEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class UniMarketDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var Instance: UniMarketDatabase? = null

        fun getDatabase(context: Context): UniMarketDatabase {
            return Instance ?: synchronized(this) {
                Instance ?: Room.databaseBuilder(context, UniMarketDatabase::class.java, "unimarket_database")
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
