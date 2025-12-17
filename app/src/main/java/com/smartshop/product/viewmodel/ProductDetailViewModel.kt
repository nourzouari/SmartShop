// ProductDetailViewModel.kt - Version améliorée
package com.smartshop.product.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.smartshop.product.repository.ProductRepository
import com.smartshop.product.model.Product

class ProductDetailViewModel(
    private val repository: ProductRepository
) : ViewModel() {

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isEditing = MutableStateFlow(false)
    val isEditing: StateFlow<Boolean> = _isEditing

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog

    // Charger un produit par son ID
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val productResult = repository.getProductById(productId)
                _product.value = productResult

                if (productResult == null) {
                    _error.value = "Produit introuvable"
                }
            } catch (e: Exception) {
                _error.value = "Erreur de chargement: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Mettre à jour la quantité
    fun updateProductQuantity(newQuantity: Int) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val currentProduct = _product.value
                if (currentProduct != null) {
                    val updatedProduct = currentProduct.copy(quantity = newQuantity)
                    repository.updateProduct(updatedProduct)
                    _product.value = updatedProduct
                }
            } catch (e: Exception) {
                _error.value = "Erreur de mise à jour: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Mettre à jour le produit complet
    fun updateProduct(updatedProduct: Product) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                repository.updateProduct(updatedProduct)
                _product.value = updatedProduct
                _isEditing.value = false
            } catch (e: Exception) {
                _error.value = "Erreur de modification: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Supprimer le produit
    fun deleteProduct() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val currentProduct = _product.value
                if (currentProduct != null) {
                    repository.deleteProduct(currentProduct)
                    _product.value = null
                    _showDeleteDialog.value = false
                }
            } catch (e: Exception) {
                _error.value = "Erreur de suppression: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Gérer l'édition
    fun startEditing() {
        _isEditing.value = true
    }

    fun cancelEditing() {
        _isEditing.value = false
    }

    // Gérer la boîte de dialogue de suppression
    fun showDeleteConfirmation() {
        _showDeleteDialog.value = true
    }

    fun hideDeleteConfirmation() {
        _showDeleteDialog.value = false
    }

    // Effacer les erreurs
    fun clearError() {
        _error.value = null
    }

    // Réinitialiser l'écran
    fun resetScreen() {
        _product.value = null
        _isEditing.value = false
        _showDeleteDialog.value = false
        _error.value = null
    }

    // Ajouter au stock
    fun addStock(amount: Int) {
        val currentProduct = _product.value
        if (currentProduct != null) {
            val newQuantity = currentProduct.quantity + amount
            updateProductQuantity(newQuantity)
        }
    }

    // Retirer du stock
    fun removeStock(amount: Int) {
        val currentProduct = _product.value
        if (currentProduct != null) {
            val newQuantity = maxOf(0, currentProduct.quantity - amount)
            updateProductQuantity(newQuantity)
        }
    }
}