package com.example.ricarica.profile

import androidx.lifecycle.ViewModel
import com.example.ricarica.rental.Rental
import com.example.ricarica.dati.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    data object Loading : AuthState()
    data object LoggedOut : AuthState()
    data class LoggedIn(val uid: String) : AuthState()
}



class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    private var profileListener: ValueEventListener? = null

    init {

        auth.addAuthStateListener { fb ->

            val user = fb.currentUser
            if (user == null) {
                detachProfileListener()
                _userProfile.value = null
                _authState.value = AuthState.LoggedOut
            } else {
                _authState.value = AuthState.LoggedIn(user.uid)
                attachProfileListener(user.uid)
            }
        }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e.message ?: "Errore login") }
    }

    fun register(
        email: String,
        password: String,
        username: String = "",
        rentals: Map<String, Rental> = emptyMap(),
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    onError("UID non disponibile")
                    return@addOnSuccessListener
                }

                val profile = UserProfile(
                    uid = uid,
                    email = email,
                    userName = username,
                    rentals = rentals,
                    createdAt = System.currentTimeMillis(),
                )

                // Scrivo in Realtime Database: /users/{uid}
                db.child("users").child(uid).setValue(profile)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        // Se vuoi essere super coerente: puoi anche eliminare l’utente Auth creato se fallisce il DB.
                        onError(e.message ?: "Errore salvataggio profilo su database")
                    }
            }
            .addOnFailureListener { e -> onError(e.message ?: "Errore registrazione") }
    }

    fun signOut() {
        auth.signOut()
        // l’AuthStateListener farà il resto (LoggedOut + pulizia profilo)
    }

    private fun attachProfileListener(uid: String) {
        detachProfileListener()

        val ref = db.child("users").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // IL PROFILO NON ESISTE PIÙ NEL DB
                    // Se l'utente è loggato in Auth ma non c'è nel DB, forziamo il logout
                    signOut()
                    return
                }
                _userProfile.value = snapshot.getValue(UserProfile::class.java)
            }

            override fun onCancelled(error: DatabaseError) { }
        }
        ref.addValueEventListener(listener)
        profileListener = listener
    }

    private fun detachProfileListener() {
        val uid = auth.currentUser?.uid
        val listener = profileListener ?: return
        if (uid != null) db.child("users").child(uid).removeEventListener(listener)
        profileListener = null
    }

    // Nel tuo ViewModel che gestisce l'autenticazione
    fun forceLogout() {
        Firebase.auth.signOut()

    }
}
