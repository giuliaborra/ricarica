package com.example.ricarica.rental
import PowerBank
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RentalViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().reference

    fun createRental(
        stationId: String,
        rawSelection: Map<PowerBank, Int>, // Riceviamo la mappa con i tuoi oggetti
        onSuccess: (Rental) -> Unit,
        onError: (String) -> Unit,
        onNotLoggedIn: () -> Unit
    ) {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            onNotLoggedIn()

            return
        }

        // 1. CONVERSIONE INTELLIGENTE: Da Oggetto PowerBank a Stringa per il DB
        val finalSelection = rawSelection
            .filter { it.value > 0 } // Tieni solo quantità > 0
            .mapKeys { entry ->
                // Qui decidiamo come chiamare la chiave nel database usando la tua classe
                when (entry.key) {
                    is PowerBank.Basic -> "Basic"
                    is PowerBank.Fast -> "Fast"
                    is PowerBank.Pro -> "Pro"
                    // Se aggiungi nuovi tipi in futuro, il compilatore ti avviserà qui!
                }
            }

        if (finalSelection.isEmpty()) {
            onError("Seleziona almeno un power bank")
            return
        }

        viewModelScope.launch {
            try {

                val rentalKey = dbRef.child("rentals").push().key ?: return@launch
                val passkey = (1000..9999).random().toString() //creo codice univoco che mi servirà per sbloccare il locker
                val now = System.currentTimeMillis()
                val expiryTime = now + (20 * 60 * 1000)

                // 2. CREAZIONE DEL NOLEGGIO
                val newRental = Rental(
                    rentalId = rentalKey,
                    userId = currentUser.uid,
                    stationId = stationId,
                    powerBankTypes = finalSelection, // mappa convertita con ESEMPIO --> { Basic: 0, Fast: 1, Pro:1}
                    passkey = passkey,
                    state = "RESERVED",
                    startTime = now,
                    endTime = expiryTime
                )

                // 3. SALVATAGGIO (Identico a prima)
                val childUpdates = hashMapOf<String, Any>()
                childUpdates["/rentals/$rentalKey"] = newRental
                childUpdates["/users/${currentUser.uid}/rentals/$rentalKey"] = newRental
                childUpdates["/stations/$stationId/rentals/$rentalKey"] = newRental

                childUpdates["/stations/$stationId/lockers/"]

                dbRef.updateChildren(childUpdates).await()
                onSuccess(newRental)

            } catch (e: Exception) {
                onError("Errore: ${e.message}")
            }
        }
    }
}