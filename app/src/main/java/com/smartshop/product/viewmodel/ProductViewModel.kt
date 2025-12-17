// ProductViewModel.kt - VERSION FINALE
package com.smartshop.product.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.smartshop.product.repository.ProductRepository
import com.smartshop.product.model.Product
import com.smartshop.product.repository.ProductStatistics
import kotlinx.coroutines.flow.asStateFlow

class ProductViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _statistics = MutableStateFlow<ProductStatistics?>(null)
    val statistics: StateFlow<ProductStatistics?> = _statistics.asStateFlow()

    init {
        loadProducts()
        loadStatistics()

        // Observer les changements en temps réel
        viewModelScope.launch {
            repository.getAllProductsFlow().collect { productList ->
                _products.value = productList
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _products.value = repository.getProducts()
            } catch (e: Exception) {
                _error.value = "Erreur de chargement: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // AJOUTER UN PRODUIT
    fun addProduct(product: Product) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null

            try {
                val productId = repository.addProduct(product)
                _successMessage.value = "Produit ajouté avec succès"

                // Pas besoin de reload si on observe le Flow
                // Le Flow va automatiquement se mettre à jour
            } catch (e: Exception) {
                _error.value = "Erreur d'ajout: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // MODIFIER UN PRODUIT
    fun updateProduct(product: Product) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null

            try {
                repository.updateProduct(product)
                _successMessage.value = "Produit modifié avec succès"
            } catch (e: Exception) {
                _error.value = "Erreur de modification: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // SUPPRIMER UN PRODUIT
    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null

            try {
                repository.deleteProduct(product)
                _successMessage.value = "Produit supprimé avec succès"
            } catch (e: Exception) {
                _error.value = "Erreur de suppression: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncWithCloud() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.syncWithFirestore()
                _successMessage.value = "Synchronisation réussie"
            } catch (e: Exception) {
                _error.value = "Erreur de synchronisation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadStatistics() {
        viewModelScope.launch {
            try {
                _statistics.value = repository.getStatistics()
            } catch (e: Exception) {
                println("DEBUG: Error loading statistics: ${e.message}")
            }
        }
    }

    fun searchProducts(query: String): List<Product> {
        return if (query.isBlank()) {
            _products.value
        } else {
            _products.value.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                        product.description.contains(query, ignoreCase = true) ||
                        product.category.contains(query, ignoreCase = true)
            }
        }
    }

    // TROUVER UN PRODUIT PAR ID
    fun getProductById(productId: String): Product? {
        return _products.value.find { it.id == productId }
    }

    // MÉTHODES UTILES POUR LES STATISTIQUES
    fun getTotalProducts(): Int = _products.value.size

    fun getTotalStockValue(): Double = _products.value.sumOf { it.price * it.quantity }

    fun getLowStockProducts(threshold: Int = 10): List<Product> {
        return _products.value.filter { it.quantity in 1 until threshold }
    }

    fun getOutOfStockProducts(): List<Product> {
        return _products.value.filter { it.quantity == 0 }
    }

    // EFFACER LES MESSAGES
    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearAllMessages() {
        _error.value = null
        _successMessage.value = null
    }
}