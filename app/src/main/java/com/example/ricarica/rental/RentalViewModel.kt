package com.example.ricarica.rental

import PowerBank
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ricarica.data.model.Station
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RentalViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().reference


    fun createRental(
        station: Station,
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
                    is PowerBank.BASIC -> "BASIC"
                    is PowerBank.FAST -> "FAST"
                    is PowerBank.PRO -> "PRO"
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
                val passkey = (1000..9999).random()
                    .toString() //creo codice univoco che mi servirà per sbloccare il locker
                val now = System.currentTimeMillis()
                val expiryTime = now + (20 * 60 * 1000)

                // 2. CREAZIONE DEL NOLEGGIO
                val newRental = Rental(
                    rentalId = rentalKey,
                    userId = currentUser.uid,
                    stationId = station.stationId,
                    powerBankTypes = finalSelection, // mappa convertita con ESEMPIO --> { Basic: 0, Fast: 1, Pro:0}
                    passkey = passkey,
                    state = "RESERVED",
                    startTime = now,
                    endTime = expiryTime
                )

                // 3. SALVATAGGIO
                val childUpdates = hashMapOf<String, Any>()
                childUpdates["/rentals/$rentalKey"] = newRental
                childUpdates["/users/${currentUser.uid}/rentals/$rentalKey"] = newRental
                childUpdates["/stations/${station.stationId}/rentals/$rentalKey"] = newRental

                val matchingLockers = station.lockers.filter {
                    it.value.state == "OCCUPIED" &&
                            finalSelection.filter { it.value != 0 }.map { it.key }
                                .contains(it.value.type)
                }.entries.groupBy {
                    it.value.type
                }

                finalSelection.filter { it.value != 0 }.forEach {
                    if (matchingLockers.contains(it.key) && ((matchingLockers[it.key]?.size
                            ?: 0) >= it.value)
                    ) {
                        //take it.value lockers for that key
                        val choosenLocker = matchingLockers[it.key]?.take(it.value)
                        choosenLocker?.forEach {
                            childUpdates["/stations/${station.stationId}/lockers/${it.value.lockerId}/state"] =
                                "RESERVED"
                            childUpdates["/stations/${station.stationId}/lockers/${it.value.lockerId}/passKey"] =
                                passkey

                        }
                    }
                }


                println("L ${matchingLockers}")

                dbRef.updateChildren(childUpdates).await()
                onSuccess(newRental)

            } catch (e: Exception) {
                onError("Errore: ${e.message}")
            }
        }
    }

    fun deleteReserved(rental: Rental?) {
        if (rental == null) return

        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            val targetPasskey = rental.passkey

            // 1. Recuperiamo i locker aggiornati direttamente dalla stazione nel DB
            val stationRef = dbRef.child("stations").child(rental.stationId).child("lockers")
            val snapshot = stationRef.get().await() // Richiede l'import di kotlinx.coroutines.tasks.await

            val childUpdates = hashMapOf<String, Any?>()

            // 2. Eliminiamo i record del noleggio
            childUpdates["/rentals/${rental.rentalId}"] = null
            childUpdates["/users/${currentUser.uid}/rentals/${rental.rentalId}"] = null
            childUpdates["/stations/${rental.stationId}/rentals/${rental.rentalId}"] = null

            // 3. Scorriamo i locker reali del database per trovare quelli con la passkey corretta
            snapshot.children.forEach { lockerSnapshot ->
                val lockerId = lockerSnapshot.key
                val currentPassKey = lockerSnapshot.child("passKey").getValue(String::class.java)

                if (currentPassKey == targetPasskey) {
                    childUpdates["/stations/${rental.stationId}/lockers/$lockerId/state"] = "OCCUPIED"
                    childUpdates["/stations/${rental.stationId}/lockers/$lockerId/passKey"] = null
                }
            }

            dbRef.updateChildren(childUpdates).addOnSuccessListener {
                println("Noleggio cancellato e locker della stazione ${rental.stationId} liberati")
            }
        }
    }


    fun confirmPickup(rental: Rental) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val stationId = rental.stationId // Salviamo l'ID per comodità

                // 1. Definiamo i percorsi di aggiornamento base (Noleggio attivo)
                val updates = hashMapOf<String, Any?>()
                updates["/rentals/${rental.rentalId}/state"] = "ACTIVE"
                updates["/users/${currentUser.uid}/rentals/${rental.rentalId}/state"] = "ACTIVE"
                updates["/stations/$stationId/rentals/${rental.rentalId}/state"] = "ACTIVE"

                // 2. SCARICHIAMO i locker dal database per trovare quello giusto
                // Puntiamo al nodo "lockers" della stazione specifica
                val lockersRef = dbRef.child("stations").child(stationId).child("lockers")

                // .get().await() scarica i dati in modo sincrono (dentro la coroutine)
                val snapshot = lockersRef.get().await()

                // 3. Iteriamo sullo SNAPSHOT (i dati reali dal DB)
                for (lockerSnapshot in snapshot.children) {
                    // Leggiamo la passKey salvata nel db per questo locker
                    val storedPasskey = lockerSnapshot.child("passKey").getValue(String::class.java)
                    val lockerId = lockerSnapshot.key

                    // Se la passkey del locker corrisponde a quella del noleggio...
                    if (storedPasskey == rental.passkey && lockerId != null) {
                        // ...aggiorniamo SOLO questo locker
                        updates["/stations/$stationId/lockers/$lockerId/state"] = "FREE"
                        // Opzionale: Rimuoviamo la passkey dal locker visto che è stato aperto
                        updates["/stations/$stationId/lockers/$lockerId/passKey"] = null
                    }
                }

                // 4. Eseguiamo tutti gli aggiornamenti in una volta sola (atomico)
                dbRef.updateChildren(updates).await()
                println("Ordine confermato e locker aggiornati")

            } catch (e: Exception) {
                println("Errore durante la conferma ritiro: ${e.message}")
            }
        }
    }
}