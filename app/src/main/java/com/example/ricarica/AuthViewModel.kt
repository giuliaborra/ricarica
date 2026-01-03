package com.example.ricarica

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    data object Loading : AuthState()
    data object LoggedOut : AuthState()
    data class LoggedIn(val uid: String) : AuthState()
}
class AuthViewModel : ViewModel() {
    private val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    private val _authState = kotlinx.coroutines.flow.MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    init {
        auth.addAuthStateListener { fb ->
            val user = fb.currentUser
            _authState.value = if (user == null) AuthState.LoggedOut else AuthState.LoggedIn(user.uid)
        }
    }
}
