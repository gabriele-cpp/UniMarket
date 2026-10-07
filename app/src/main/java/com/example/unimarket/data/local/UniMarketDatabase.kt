package com.example.unimarket.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ProductEntity::class], version = 3, exportSchema = false)
abstract class UniMarketDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var Instance: UniMarketDatabase? = null

        fun getDatabase(context: Context): UniMarketDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, UniMarketDatabase::class.java, "unimarket_database")
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
