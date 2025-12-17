// app/src/main/java/com/smartshop/product/data/local/database/AppDatabase.kt
package com.smartshop.product.data.local

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartshop.product.data.local.entity.LocalProduct

@Database(
    entities = [LocalProduct::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartshop_database"
                )
                    .fallbackToDestructiveMigration() // En développement seulement
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Pour les tests
        @VisibleForTesting
        fun createTestDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            ).build()
        }
    }
}