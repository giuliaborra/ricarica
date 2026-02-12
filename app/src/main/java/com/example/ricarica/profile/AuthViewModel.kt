package com.example.ricarica.profile

import androidx.lifecycle.ViewModel
import com.example.ricarica.dati.UserProfile
import com.example.ricarica.rental.Rental
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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

    // --- Stati ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    // Liste Noleggi
    private val _activeRentals = MutableStateFlow<List<Rental>>(emptyList())
    val activeRentals = _activeRentals.asStateFlow()

    private val _rentalHistory = MutableStateFlow<List<Rental>>(emptyList())
    val rentalHistory = _rentalHistory.asStateFlow()
    // -------------

    private var profileListener: ValueEventListener? = null
    private var rentalListener: ValueEventListener? = null

    init {
        auth.addAuthStateListener { fb ->
            val user = fb.currentUser
            if (user == null) {
                detachListeners()
                _userProfile.value = null
                _activeRentals.value = emptyList()
                _rentalHistory.value = emptyList()
                _authState.value = AuthState.LoggedOut
            } else {
                _authState.value = AuthState.LoggedIn(user.uid)
                attachListeners(user.uid)
            }
        }
    }

    // --- Login & Register ---
    fun login(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) { onError("Compila tutti i campi"); return }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Errore login") }
    }

    fun register(email: String, pass: String, userName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || pass.isBlank()) { onError("Compila tutti i campi"); return }
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { res ->
                res.user?.uid?.let { uid ->
                    val profile = UserProfile(userName = userName, email = email)
                    db.child("users").child(uid).setValue(profile).addOnSuccessListener { onSuccess() }
                }
            }
            .addOnFailureListener { onError(it.message ?: "Errore registrazione") }
    }

    fun signOut() {
        auth.signOut()
    }

    // --- Listener Firebase ---
    private fun attachListeners(uid: String) {
        detachListeners()

        // 1. Profilo
        val profileRef = db.child("users").child(uid)
        val pListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) _userProfile.value = snapshot.getValue(UserProfile::class.java)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        profileRef.addValueEventListener(pListener)
        profileListener = pListener

        // 2. Noleggi (Logica Aggiornata)
        val rentalsRef = db.child("users").child(uid).child("rentals")
        val rListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allRentals = snapshot.children.mapNotNull { it.getValue(Rental::class.java) }

                // LISTA 1: ATTIVI (Per la Home/Mappa)
                // Include quelli in corso, prenotati o in restituzione
                _activeRentals.value = allRentals.filter {
                    it.state == "ACTIVE" || it.state == "RESERVED" || it.state == "RETURNING"
                }.sortedByDescending { it.startTime }

                // LISTA 2: STORICO (Per il Profilo)
                // Come richiesto: Filtriamo per ACTIVE o COMPLETED
                _rentalHistory.value = allRentals.filter {
                    it.state == "ACTIVE" || it.state == "COMPLETED"
                }.sortedByDescending { it.endTime ?: it.startTime }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rentalsRef.addValueEventListener(rListener)
        rentalListener = rListener
    }

    private fun detachListeners() {
        val uid = auth.currentUser?.uid ?: return
        if (profileListener != null) db.child("users").child(uid).removeEventListener(profileListener!!)
        if (rentalListener != null) db.child("users").child(uid).child("rentals").removeEventListener(rentalListener!!)
        profileListener = null
        rentalListener = null
    }


    // Aggiungi questi import

    // Dentro la classe AuthViewModel
    fun updatePassword(currentPass: String, newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser

        if (user != null && user.email != null) {

            // 1. Creiamo le credenziali con la password ATTUALE
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)

            // 2. Tenta di ri-autenticare l'utente
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // 3. Se la vecchia password è giusta, aggiorniamo con la NUOVA
                    user.updatePassword(newPass)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onError("Errore aggiornamento: ${e.localizedMessage}")
                        }
                }
                .addOnFailureListener { e ->
                    // Qui finiamo se la password attuale è sbagliata
                    onError("La password attuale non è corretta.")
                }
        } else {
            onError("Utente non loggato.")
        }
    }
}