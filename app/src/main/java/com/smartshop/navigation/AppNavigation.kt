// AppNavigation.kt - VERSION CORRIGÉE
package com.smartshop.navigation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartshop.auth.LoginScreen
import com.smartshop.product.viewmodel.ProductViewModel
import com.smartshop.product.ui.ProductListScreen
import com.smartshop.product.ui.ProductDetailScreen
import com.smartshop.product.ui.ProductFormScreen
import com.smartshop.product.repository.ProductRepository
import com.smartshop.product.viewmodel.ProductViewModelFactory
import com.smartshop.product.viewmodel.ProductDetailViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smartshop.auth.AuthStateHolder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch // <-- CET IMPORT EST ESSENTIEL

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val repository = remember {
        ProductRepository(context)
    }

    val viewModelFactory = remember {
        ProductViewModelFactory(repository)
    }



    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }


        composable("home") {
            val productViewModel: ProductViewModel = viewModel(
                factory = viewModelFactory
            )

            ProductListScreen(
                viewModel = productViewModel,
                onEditProduct = { product ->
                    if (product == null) {
                        navController.navigate("product_form/new")
                    } else {
                        navController.navigate("product_form/${product.id}")
                    }
                },
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                }
            )
        }

        composable(
            route = "product_form/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""

            // Utiliser ProductDetailViewModel pour charger le produit spécifique
            val detailViewModel: ProductDetailViewModel = viewModel(factory = viewModelFactory)
            val product by detailViewModel.product.collectAsStateWithLifecycle()
            val isLoading by detailViewModel.isLoading.collectAsStateWithLifecycle()

            // ProductViewModel pour sauvegarder
            val productViewModel: ProductViewModel = viewModel(factory = viewModelFactory)

            // Charger le produit spécifique si ce n'est pas un nouveau
            if (productId != "new") {
                LaunchedEffect(productId) {
                    detailViewModel.loadProduct(productId)
                }
            }

            // Écran de chargement pendant le chargement du produit
            if (isLoading && productId != "new") {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ProductFormScreen(
                    product = if (productId == "new") null else product,
                    onSave = { updatedProduct ->
                        // Utilisez le coroutineScope pour lancer la coroutine
                        coroutineScope.launch {
                            if (productId == "new") {
                                productViewModel.addProduct(updatedProduct)
                            } else {
                                productViewModel.updateProduct(updatedProduct)
                            }
                            navController.navigateUp()
                        }
                    },
                    onCancel = {
                        navController.navigateUp()
                    }
                )
            }
        }

        composable(
            route = "product_detail/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val detailViewModel: ProductDetailViewModel = viewModel(
                factory = viewModelFactory
            )

            ProductDetailScreen(
                productId = productId,
                onBackClick = { navController.navigateUp() },
                onEditClick = { editProductId ->
                    navController.navigate("product_form/$editProductId")
                },
                viewModel = detailViewModel,
                onProductDeleted = {
                    navController.navigateUp()
                }
            )
        }
    }
}