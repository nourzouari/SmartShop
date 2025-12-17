package com.smartshop.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    var state by mutableStateOf(LoginState())
        private set

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun onEmailChanged(newEmail: String) {
        state = state.copy(email = newEmail, error = null)
    }

    fun onPasswordChanged(newPass: String) {
        state = state.copy(password = newPass, error = null)
    }

    fun login() {
        // Validation
        if (state.email.isBlank() || state.password.isBlank()) {
            state = state.copy(error = "Veuillez remplir tous les champs.")
            return
        }

        state = state.copy(isLoading = true, error = null, isSuccess = false)

        // Utiliser viewModelScope pour les coroutines
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(state.email, state.password)
                    .await() // Attendre la fin de l'opération Firebase

                // Succès : mettre à jour le state
                state = state.copy(
                    isLoading = false,
                    isSuccess = true,
                    error = null
                )

            } catch (e: Exception) {
                // Gérer les erreurs spécifiques
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidUserException -> "Utilisateur non trouvé"
                    is FirebaseAuthInvalidCredentialsException -> "Email ou mot de passe incorrect"
                    else -> "Erreur de connexion: ${e.localizedMessage}"
                }

                state = state.copy(
                    isLoading = false,
                    isSuccess = false,
                    error = errorMessage
                )
            }
        }
    }

    fun resetState() {
        state = LoginState()
    }
}