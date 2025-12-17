package com.smartshop.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.smartshop.navigation.NavigationViewModel

class NavigationViewModel : ViewModel() {
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Login)
    val currentScreen: StateFlow<Screen> = _currentScreen

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun goBack() {
        when (_currentScreen.value) {
            is Screen.Profile, is Screen.Settings -> _currentScreen.value = Screen.Home
            else -> _currentScreen.value = Screen.Login
        }
    }
}