package com.seekersofjustice.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.seekersofjustice.app.App
import com.seekersofjustice.app.data.repo.AuthRepository
import com.seekersofjustice.app.domain.model.Officer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    val officers: StateFlow<List<Officer>> = auth.observeOfficers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** null = still loading; true = no officers yet (first-run setup). */
    private val _needsSetup = MutableStateFlow<Boolean?>(null)
    val needsSetup: StateFlow<Boolean?> = _needsSetup

    init {
        viewModelScope.launch { _needsSetup.value = !auth.hasAnyOfficer() }
    }

    fun createOfficer(name: String, pin: String, onCreated: (Officer) -> Unit) {
        viewModelScope.launch { onCreated(auth.createOfficer(name, pin)) }
    }

    fun signIn(officerId: Long, pin: String, onResult: (Officer?) -> Unit) {
        viewModelScope.launch {
            val ok = auth.verifyPin(officerId, pin)
            onResult(if (ok) auth.getOfficer(officerId) else null)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as App
                AuthViewModel(app.container.authRepository)
            }
        }
    }
}
