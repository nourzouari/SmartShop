// ProductMapper.kt - VERSION CORRIGÉE
package com.smartshop.product.data.local

import com.smartshop.product.data.local.entity.LocalProduct
import com.smartshop.product.model.Product
import com.google.firebase.Timestamp
import java.util.Date

object ProductMapper {

    // === CONVERSION LOCAL → UI ===

    // Fonction principale pour convertir LocalProduct → Product
    fun toProduct(localProduct: LocalProduct): Product {
        return Product(
            id = localProduct.id,
            name = localProduct.name,
            description = localProduct.description,
            price = localProduct.price,
            quantity = localProduct.quantity,
            category = localProduct.category,
            imageUrl = localProduct.imageUrl,
            userId = localProduct.userId,
            createdAt = Timestamp(Date(localProduct.createdAt))
        )
    }

    // === CONVERSION UI → LOCAL ===

    // Fonction principale pour convertir Product → LocalProduct
    fun toLocalProduct(product: Product, isSynced: Boolean = true): LocalProduct {
        val now = System.currentTimeMillis()
        return LocalProduct(
            id = if (product.id.isNotEmpty()) product.id else generateLocalId(),
            name = product.name,
            description = product.description,
            price = product.price,
            quantity = product.quantity,
            category = product.category,
            imageUrl = product.imageUrl,
            userId = product.userId,
            createdAt = product.createdAt?.toDate()?.time ?: now,
            updatedAt = now,
            isSynced = isSynced,
            isDeleted = false,
            lastSyncAttempt = 0,
            syncError = ""
        )
    }

    // === MISE À JOUR ===

    // Mise à jour d'un LocalProduct existant
    fun updateLocalProduct(existing: LocalProduct, updated: Product): LocalProduct {
        return existing.copy(
            name = updated.name,
            description = updated.description,
            price = updated.price,
            quantity = updated.quantity,
            category = updated.category,
            imageUrl = updated.imageUrl,
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
    }

    // === GÉNÉRATION D'ID ===

    private fun generateLocalId(): String {
        return "local_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}