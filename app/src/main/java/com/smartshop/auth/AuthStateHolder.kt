// AuthStateHolder.kt - VERSION SIMPLIFIÉE SANS HILT
package com.smartshop.auth

import kotlinx.coroutines.flow.*
import com.google.firebase.auth.FirebaseAuth
// RETIREZ les imports javax.inject !

// RETIREZ @Singleton
class AuthStateHolder private constructor() {
    private val auth = FirebaseAuth.getInstance()

    private val _isUserLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _isUserLoggedIn.value = firebaseAuth.currentUser != null
        }
    }

    // Pattern singleton manuel (remplace @Singleton)
    companion object {
        @Volatile
        private var INSTANCE: AuthStateHolder? = null

        fun getInstance(): AuthStateHolder {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthStateHolder().also { INSTANCE = it }
            }
        }
    }
}