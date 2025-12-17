// ProductRepository.kt - VERSION CORRIGÉE
package com.smartshop.product.repository

import android.content.Context
import com.smartshop.product.data.local.AppDatabase
import com.smartshop.product.data.local.ProductDao
import com.smartshop.product.data.local.entity.LocalProduct
import com.smartshop.product.data.local.ProductMapper
import com.smartshop.product.model.Product
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProductRepository(
    private val context: Context
) {

    // Firestore
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private fun getProductsCollection() = db.collection("products")

    // Room
    private val database = AppDatabase.getInstance(context)
    private val productDao: ProductDao = database.productDao()

    // Scope pour les opérations asynchrones
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // ==================== MÉTHODES FIRESTORE (CLOUD) ====================

    suspend fun getProductsFromFirestore(): List<Product> {
        return try {
            val snapshot = getProductsCollection().get().await()

            println("DEBUG: Found ${snapshot.documents.size} documents in Firestore")

            snapshot.documents.mapNotNull { document ->
                try {
                    document.toObject(Product::class.java)?.copy(id = document.id)
                } catch (e: Exception) {
                    println("DEBUG: Error parsing Firestore document ${document.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Exception in getProductsFromFirestore: ${e.message}")
            emptyList()
        }
    }

    suspend fun getProductByIdFromFirestore(productId: String): Product? {
        return try {
            val document: DocumentSnapshot = getProductsCollection()
                .document(productId)
                .get()
                .await()

            if (document.exists()) {
                document.toObject(Product::class.java)?.copy(id = document.id)
            } else {
                null
            }
        } catch (e: Exception) {
            println("DEBUG: Exception in getProductByIdFromFirestore: ${e.message}")
            null
        }
    }

    suspend fun addProductToFirestore(product: Product): String {
        return try {
            val currentUser = auth.currentUser
            val productData = if (currentUser != null) {
                product.copy(userId = currentUser.uid)
            } else {
                product
            }

            val productMap = mapOf(
                "name" to productData.name,
                "description" to productData.description,
                "price" to productData.price,
                "quantity" to productData.quantity,
                "category" to productData.category,
                "imageUrl" to productData.imageUrl,
                "userId" to productData.userId,
                "createdAt" to Timestamp.now()
            )

            val documentRef = getProductsCollection().add(productMap).await()
            documentRef.id
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateProductInFirestore(product: Product) {
        try {
            val productMap = mapOf(
                "name" to product.name,
                "description" to product.description,
                "price" to product.price,
                "quantity" to product.quantity,
                "category" to product.category,
                "imageUrl" to product.imageUrl
            )

            getProductsCollection()
                .document(product.id)
                .update(productMap)
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteProductFromFirestore(productId: String) {
        try {
            getProductsCollection()
                .document(productId)
                .delete()
                .await()
        } catch (e: Exception) {
            throw e
        }
    }

    // ==================== MÉTHODES ROOM (LOCAL) ====================

    suspend fun insertLocalProduct(product: Product): String {
        val localProduct = ProductMapper.toLocalProduct(product, isSynced = false)
        productDao.insert(localProduct)
        return localProduct.id
    }

    suspend fun updateLocalProduct(product: Product) {
        val existing = productDao.getProductById(product.id)
        if (existing != null) {
            val updated = ProductMapper.updateLocalProduct(existing, product)
            productDao.update(updated)
        } else {
            insertLocalProduct(product)
        }
    }

    suspend fun deleteLocalProduct(productId: String) {
        productDao.softDelete(productId, System.currentTimeMillis())
    }

    fun getLocalProducts(): Flow<List<Product>> {
        return productDao.getAllProducts().map { localProducts ->
            localProducts.map { localProduct ->
                ProductMapper.toProduct(localProduct)
            }
        }
    }

    fun getAllProductsFlow(): Flow<List<Product>> {
        return productDao.getAllProducts().map { localProducts ->
            localProducts.map { localProduct ->
                ProductMapper.toProduct(localProduct)
            }
        }
    }

    suspend fun getLocalProductById(productId: String): Product? {
        return productDao.getProductById(productId)?.let { localProduct ->
            ProductMapper.toProduct(localProduct)
        }
    }

    // ==================== SYNCHRONISATION ====================

    suspend fun syncWithFirestore() {
        try {
            // 1. Télécharger depuis Firestore
            val firestoreProducts = getProductsFromFirestore()

            // 2. Sauvegarder localement
            val localProducts = firestoreProducts.map { product ->
                ProductMapper.toLocalProduct(product, isSynced = true)
            }

            if (localProducts.isNotEmpty()) {
                productDao.insertAll(localProducts)
            }

            // 3. Uploader les modifications locales non synchronisées
            val unsyncedProducts = productDao.getUnsyncedProducts()
            for (localProduct in unsyncedProducts) {
                try {
                    val product = ProductMapper.toProduct(localProduct)
                    if (localProduct.isDeleted) {
                        deleteProductFromFirestore(localProduct.id)
                        productDao.hardDeleteIfSynced(localProduct.id)
                    } else {
                        addProductToFirestore(product)
                        productDao.markAsSynced(localProduct.id)
                    }
                } catch (e: Exception) {
                    println("DEBUG: Sync error for ${localProduct.id}: ${e.message}")
                }
            }

        } catch (e: Exception) {
            println("DEBUG: Exception in syncWithFirestore: ${e.message}")
            throw e
        }
    }

    suspend fun forceSync() {
        productDao.deleteAll()
        syncWithFirestore()
    }

    // ==================== MÉTHODES COMBINÉES ====================

    suspend fun getProducts(): List<Product> {
        return try {
            val localProductsFlow = productDao.getAllProducts()
            val localProducts = localProductsFlow.first()

            if (localProducts.isNotEmpty()) {
                localProducts.map { localProduct ->
                    ProductMapper.toProduct(localProduct)
                }
            } else {
                syncWithFirestore()

                val syncedProducts = productDao.getAllProducts().first()
                syncedProducts.map { localProduct ->
                    ProductMapper.toProduct(localProduct)
                }
            }
        } catch (e: Exception) {
            println("DEBUG: Exception in getProducts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getProductById(productId: String): Product? {
        return try {
            val localProduct = getLocalProductById(productId)

            if (localProduct != null) {
                localProduct
            } else {
                getProductByIdFromFirestore(productId)
            }
        } catch (e: Exception) {
            println("DEBUG: Exception in getProductById: ${e.message}")
            null
        }
    }

    suspend fun addProduct(product: Product): String {
        return try {
            // Double écriture : d'abord local, puis cloud
            val localId = insertLocalProduct(product)

            // Synchroniser en arrière-plan avec le scope IO
            ioScope.launch {
                try {
                    val cloudId = addProductToFirestore(product.copy(id = localId))
                    if (cloudId != localId) {
                        // Gérer le mapping des IDs
                    }
                    productDao.markAsSynced(localId)
                } catch (e: Exception) {
                    println("DEBUG: Background sync failed: ${e.message}")
                }
            }

            localId
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateProduct(product: Product) {
        try {
            // Mettre à jour localement
            updateLocalProduct(product)

            // Synchroniser en arrière-plan avec le scope IO
            ioScope.launch {
                try {
                    updateProductInFirestore(product)
                    productDao.markAsSynced(product.id)
                } catch (e: Exception) {
                    println("DEBUG: Background sync failed for update: ${e.message}")
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteProduct(product: Product) {
        try {
            // Soft delete local
            deleteLocalProduct(product.id)

            // Synchroniser en arrière-plan avec le scope IO
            ioScope.launch {
                try {
                    deleteProductFromFirestore(product.id)
                    productDao.hardDeleteIfSynced(product.id)
                } catch (e: Exception) {
                    println("DEBUG: Background sync failed for delete: ${e.message}")
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    // ==================== STATISTIQUES ====================

    suspend fun getStatistics(): ProductStatistics {
        return ProductStatistics(
            totalProducts = productDao.getTotalProductCount(),
            totalStockValue = productDao.getTotalStockValue() ?: 0.0,
            totalQuantity = productDao.getTotalQuantity() ?: 0,
            lowStockCount = productDao.getLowStockCount(),
            outOfStockCount = productDao.getOutOfStockCount()
        )
    }

    // ==================== RECHERCHE ====================

    fun searchProducts(query: String): Flow<List<Product>> {
        return productDao.getAllProducts().map { localProducts ->
            localProducts.filter { localProduct ->
                localProduct.name.contains(query, ignoreCase = true) ||
                        localProduct.description.contains(query, ignoreCase = true) ||
                        localProduct.category.contains(query, ignoreCase = true)
            }.map { localProduct ->
                ProductMapper.toProduct(localProduct)
            }
        }
    }
}

data class ProductStatistics(
    val totalProducts: Int,
    val totalStockValue: Double,
    val totalQuantity: Int,
    val lowStockCount: Int,
    val outOfStockCount: Int
)