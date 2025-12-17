package com.smartshop.product.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.smartshop.product.data.local.entity.LocalProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    // --- Opérations CRUD de base ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: LocalProduct)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<LocalProduct>)

    @Update
    suspend fun update(product: LocalProduct)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun delete(productId: String)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    // --- Requêtes de lecture ---

    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<LocalProduct>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): LocalProduct?

    @Query("SELECT * FROM products WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getProductsByUser(userId: String): Flow<List<LocalProduct>>

    @Query("SELECT * FROM products WHERE category = :category AND isDeleted = 0 ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<LocalProduct>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<LocalProduct>>

    // --- Gestion de la synchronisation ---

    @Query("SELECT * FROM products WHERE isSynced = 0 AND isDeleted = 0")
    suspend fun getUnsyncedProducts(): List<LocalProduct>

    @Query("SELECT * FROM products WHERE isDeleted = 1 AND isSynced = 0")
    suspend fun getDeletedButNotSyncedProducts(): List<LocalProduct>

    @Query("UPDATE products SET isSynced = 1 WHERE id = :productId")
    suspend fun markAsSynced(productId: String)

    @Query("UPDATE products SET lastSyncAttempt = :timestamp, syncError = :error WHERE id = :productId")
    suspend fun updateSyncAttempt(productId: String, timestamp: Long, error: String = "")

    // Soft delete (marquer comme supprimé)
    @Query("UPDATE products SET isDeleted = 1, updatedAt = :timestamp WHERE id = :productId")
    suspend fun softDelete(productId: String, timestamp: Long = System.currentTimeMillis())

    // Hard delete (supprimer vraiment)
    @Query("DELETE FROM products WHERE id = :productId AND isSynced = 1")
    suspend fun hardDeleteIfSynced(productId: String)

    // --- Statistiques ---

    @Query("SELECT COUNT(*) FROM products WHERE isDeleted = 0")
    suspend fun getTotalProductCount(): Int

    @Query("SELECT SUM(price * quantity) FROM products WHERE isDeleted = 0")
    suspend fun getTotalStockValue(): Double?

    @Query("SELECT SUM(quantity) FROM products WHERE isDeleted = 0")
    suspend fun getTotalQuantity(): Int?

    @Query("SELECT COUNT(*) FROM products WHERE quantity < 10 AND quantity > 0 AND isDeleted = 0")
    suspend fun getLowStockCount(): Int

    @Query("SELECT COUNT(*) FROM products WHERE quantity = 0 AND isDeleted = 0")
    suspend fun getOutOfStockCount(): Int

    @Query("SELECT category, COUNT(*) as count, SUM(price * quantity) as value FROM products WHERE isDeleted = 0 GROUP BY category ORDER BY value DESC")
    suspend fun getCategoryStats(): List<CategoryStat>

    // --- Maintenance ---

    @Query("SELECT * FROM products WHERE isSynced = 0 AND lastSyncAttempt < :threshold AND (syncError = '' OR syncError LIKE '%retry%')")
    suspend fun getProductsToRetrySync(threshold: Long): List<LocalProduct>

    @Query("DELETE FROM products WHERE isDeleted = 1 AND isSynced = 1 AND updatedAt < :threshold")
    suspend fun cleanOldDeletedProducts(threshold: Long)
}

data class CategoryStat(
    val category: String,
    val count: Int,
    val value: Double
)