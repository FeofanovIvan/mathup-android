package com.feofanova.mathup.ui.screens.main


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel : ViewModel() {
    private val _selectedProfile = MutableStateFlow("База")
    val selectedProfile: StateFlow<String> = _selectedProfile

    fun updateProfile(profile: String) {
        _selectedProfile.value = profile
    }
}
