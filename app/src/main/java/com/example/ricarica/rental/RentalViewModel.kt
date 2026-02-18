package com.example.ricarica.rental

import PowerBank
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.ceil

class RentalViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().reference

    // --- NUOVO: VARIABILE REATTIVA PER LO STATO DEL LOCKER FISICO ---
    private val _targetLockerStatusFisico = MutableStateFlow<String?>(null)
    val targetLockerStatusFisico = _targetLockerStatusFisico.asStateFlow()

    private val _returnRentalState = MutableStateFlow<String?>(null)
    val returnRentalState = _returnRentalState.asStateFlow()


    private var lockerListener: ValueEventListener? = null

    // ------------------------------------------------------------------
    // FUNZIONI PER OSSERVARE LO STATO FISICO DEL LOCKER (NUOVE)
    // ------------------------------------------------------------------
    fun watchLockerStatus(stationId: String, lockerId: String) {
        // Rimuovi eventuali listener precedenti
        stopWatchingLocker(stationId, lockerId)

        val lockerRef = dbRef.child("stations").child(stationId).child("lockers").child(lockerId).child("stateFisico")

        lockerListener = object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val state = snapshot.getValue(String::class.java)
                _targetLockerStatusFisico.value = state
                println("DEBUG: Stato Locker $lockerId cambiato in: $state")
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                println("Errore watcher locker: ${error.message}")
            }
        }

        lockerRef.addValueEventListener(lockerListener!!)
    }

    fun stopWatchingLocker(stationId: String, lockerId: String) {
        lockerListener?.let {
            dbRef.child("stations").child(stationId).child("lockers").child(lockerId).child("stateFisico")
                .removeEventListener(it)
        }
        lockerListener = null
        _targetLockerStatusFisico.value = null
    }

    // ------------------------------------------------------------------
    // 1. CREAZIONE NOLEGGIO (PRENOTAZIONE)
    // ------------------------------------------------------------------
    fun createRental(
        stationId: String,
        rawSelection: Map<PowerBank, Int>,
        onSuccess: (List<Rental>) -> Unit,
        onError: (String) -> Unit,
        onNotLoggedIn: () -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onNotLoggedIn()
            return
        }

        // Mappatura della selezione (PowerBank object -> String key)
        val requestMap = rawSelection
            .filter { it.value > 0 }
            .mapKeys { entry ->
                when (entry.key) {
                    is PowerBank.BASIC -> "BASIC"
                    is PowerBank.FAST -> "FAST"
                    is PowerBank.PRO -> "PRO"
                }
            }

        if (requestMap.isEmpty()) {
            onError("Seleziona almeno un power bank")
            return
        }

        viewModelScope.launch {
            try {
                // Scarica i locker per trovare quelli disponibili
                val lockersRef = dbRef.child("stations").child(stationId).child("lockers")
                val snapshot = lockersRef.get().await()

                val availableLockers = snapshot.children.mapNotNull { doc ->
                    val state = doc.child("state").getValue(String::class.java)
                    val type = doc.child("type").getValue(String::class.java)
                    val lockerId = doc.key
                    val pbId = doc.child("powerBankId").getValue(String::class.java)

                    // Cerchiamo locker OCCUPIED (pieni) pronti per essere noleggiati
                    if (state == "OCCUPIED" && type != null && lockerId != null) {
                        TempLocker(lockerId, type, pbId)
                    } else null
                }.toMutableList()

                val now = System.currentTimeMillis()
                val expiryTime = now + (20 * 60 * 1000) // 20 min per ritirare
                val childUpdates = hashMapOf<String, Any>()
                val createdRentals = mutableListOf<Rental>()

                for ((type, quantity) in requestMap) {
                    val lockersForType = availableLockers.filter { it.type == type }

                    if (lockersForType.size < quantity) {
                        onError("Non ci sono abbastanza powerbank di tipo $type.")
                        return@launch
                    }

                    repeat(quantity) {
                        val chosenLocker = lockersForType.first()
                        availableLockers.remove(chosenLocker)

                        val rentalKey = dbRef.child("rentals").push().key ?: return@launch
                        val passkey = (1000..9999).random() // Codice numerico semplice

                        val newRental = Rental(
                            rentalId = rentalKey,
                            userId = currentUser.uid,
                            stationId = stationId,
                            powerBankTypes = mapOf(type to 1),
                            unlock_code = passkey,
                            state = "RESERVED",
                            startTime = now,
                            endTime = expiryTime,
                            powerBankId = chosenLocker.pbId,
                            type = type,
                            lockerId = chosenLocker.id
                        )

                        createdRentals.add(newRental)

                        // --- SCRITTURA DATABASE (3 PUNTI + LOCKER) ---

                        // 1. Globale (Amministrazione)
                        childUpdates["/rentals/$rentalKey"] = newRental

                        // 2. Utente (App Personale)
                        childUpdates["/users/${currentUser.uid}/rentals/$rentalKey"] = newRental

                        // 3. Stazione (Storico Locale) - MANTENUTO COME RICHIESTO
                        childUpdates["/stations/${stationId}/rentals/$rentalKey"] = newRental
                        childUpdates["/stations/${stationId}/lockers/${chosenLocker.id}/rental"] = newRental

                        // 4. Locker Fisico (Prenotazione)
                        childUpdates["/stations/${stationId}/lockers/${chosenLocker.id}/state"] = "RESERVED"
                    }
                }

                dbRef.updateChildren(childUpdates).await()
                onSuccess(createdRentals)

            } catch (e: Exception) {
                onError("Errore creazione: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // ------------------------------------------------------------------
    // 2. CANCELLAZIONE PRENOTAZIONE
    // ------------------------------------------------------------------
    fun deleteReserved(rental: Rental?) {
        if (rental == null) return

        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            val targetPasskey = rental.unlock_code

            val now = System.currentTimeMillis()

            val stationRef = dbRef.child("stations").child(rental.stationId).child("lockers")
            val snapshot = stationRef.get().await()
            val childUpdates = hashMapOf<String, Any?>()



            // Rimuoviamo il noleggio da tutte le liste
            childUpdates["/rentals/${rental.rentalId}"] = null
            childUpdates["/users/${currentUser.uid}/rentals/${rental.rentalId}"] = null
            childUpdates["/stations/${rental.stationId}/rentals/${rental.rentalId}"] = null
            childUpdates["/stations/${rental.stationId}/lockers/${rental.lockerId}/rental"] = null
            childUpdates["/stations/${rental.stationId}/lockers/${rental.lockerId}/state"] = "OCCUPIED"

            dbRef.updateChildren(childUpdates)
        }
    }

    // ------------------------------------------------------------------
    // 3. CONFERMA RITIRO (Unlock -> ACTIVE)
    // ------------------------------------------------------------------
    fun confirmPickup(rental: Rental) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val stationId = rental.stationId
                val updates = hashMapOf<String, Any?>()
                val now = System.currentTimeMillis()

                // Aggiorna stato in ACTIVE ovunque
                //updates["/rentals/${rental.rentalId}/state"] = "ACTIVE"
                updates["/users/${currentUser.uid}/rentals/${rental.rentalId}/state"] = "ACTIVE"
                updates["/stations/$stationId/rentals/${rental.rentalId}/state"] = "ACTIVE"

                //aggiorna tempo
                updates["/rentals/${rental.rentalId}/startTime"] = now
                updates["/users/${currentUser.uid}/rentals/${rental.rentalId}/startTime"] = now
                updates["/users/${currentUser.uid}/rentals/${rental.rentalId}/startTime"] = now

                //aggiorna locker
                updates["/stations/$stationId/lockers/${rental.lockerId}/rental"] = null
                updates["/stations/$stationId/lockers/${rental.lockerId}/powerBankId"] = null
                updates["/stations/$stationId/lockers/${rental.lockerId}/type"] = null
                updates["/stations/$stationId/lockers/${rental.lockerId}/state"] = "FREE"





                dbRef.updateChildren(updates).await()
            } catch (e: Exception) {
                println("Errore conferma ritiro: ${e.message}")
            }
        }
    }

    // ------------------------------------------------------------------
    // 4. PREPARAZIONE RESTITUZIONE (Cerca slot FREE nella stessa stazione)
    // ------------------------------------------------------------------
    fun prepareReturn(
        rental: Rental,
        onReadyToPlay: (Int, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val stationId = rental.stationId
                val currentUser = auth.currentUser ?: return@launch
                val lockersRef = dbRef.child("stations").child(stationId).child("lockers")
                val lockersSnapshot = lockersRef.get().await()

                var targetLockerId: String? = null
                for (locker in lockersSnapshot.children) {
                    val state = locker.child("state").getValue(String::class.java)
                    if (state == "FREE") {
                        targetLockerId = locker.key
                        break
                    }
                }

                if (targetLockerId == null) {
                    onError("Nessuno slot libero.")
                    return@launch
                }

                val returnCode = (1000..9999).random()

                // Prepariamo l'oggetto Rental da inserire nel locker
                // Arduino leggerà questo oggetto per convalidare il ritorno
                val rentalForLocker = rental.copy(
                    unlock_code = returnCode,
                    lockerId = targetLockerId,
                    state = "RETURNING" // Deve essere attivo perché Arduino lo processi
                )

                val updates = hashMapOf<String, Any?>()

                // 1. Aggiorniamo i puntatori globali
                updates["/stations/$stationId/rentals/${rental.rentalId}/lockerId"] = targetLockerId
                updates["/users/${currentUser.uid}/rentals/${rental.rentalId}/lockerId"] = targetLockerId
                updates["/rentals/${rental.rentalId}/lockerId"] = targetLockerId

                // 2. INSERIAMO IL RENTAL NEL LOCKER (Arduino lo leggerà qui)
                updates["/stations/$stationId/lockers/$targetLockerId/state"] = "RESERVED"
                updates["/stations/$stationId/lockers/$targetLockerId/rental"] = rentalForLocker

                dbRef.updateChildren(updates).await()

                onReadyToPlay(returnCode, targetLockerId)

            } catch (e: Exception) {
                onError("Errore: ${e.message}")
            }
        }
    }

    // ------------------------------------------------------------------
    // 5. CHIUSURA NOLEGGIO (COMPLETED)
    // ------------------------------------------------------------------
    fun terminateRental(
        rental: Rental,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    onError("Utente non loggato")
                    return@launch
                }

                val stationId = rental.stationId
                val rentalId = rental.rentalId
                val now = System.currentTimeMillis()

                // Calcolo Costo
                val durationMillis = now - (rental.startTime ?: now)
                val durationMinutes = ceil(durationMillis / 60000.0).toLong()

                val types = rental.powerBankTypes ?: mapOf("BASIC" to 1)
                val totalCost = calculateSingleRentalCost(types, durationMinutes)

                val updates = hashMapOf<String, Any?>()

                // AGGIORNAMENTI NOLEGGIO (COMPLETED) - SU TUTTE E 3 LE LISTE

                // 1. Globale
                updates["/rentals/$rentalId/state"] = "COMPLETED"
                updates["/rentals/$rentalId/endTime"] = now
                updates["/rentals/$rentalId/totalCost"] = totalCost

                // 2. Utente
                updates["/users/${currentUser.uid}/rentals/$rentalId/state"] = "COMPLETED"
                updates["/users/${currentUser.uid}/rentals/$rentalId/endTime"] = now
                updates["/users/${currentUser.uid}/rentals/$rentalId/totalCost"] = totalCost

                // 3. Stazione (Storico MANTENUTO)
                updates["/stations/$stationId/rentals/$rentalId/state"] = "COMPLETED"
                updates["/stations/$stationId/rentals/$rentalId/endTime"] = now
                updates["/stations/$stationId/rentals/$rentalId/totalCost"] = totalCost

                // AGGIORNAMENTO LOCKER (Diventa OCCUPIED)
                updates["/stations/$stationId/lockers/${rental.lockerId}/state"] = "OCCUPIED"
                updates["/stations/$stationId/lockers/${rental.lockerId}/powerBankId"] = rental.powerBankId
                updates["/stations/$stationId/lockers/${rental.lockerId}/type"] = rental.type

                // PULIZIA LOCKER (Rimuovi codice e rental temporaneo)
                updates["/stations/$stationId/lockers/${rental.lockerId}/unlock_code"] = null
                updates["/stations/$stationId/lockers/${rental.lockerId}/rental"] = null

                dbRef.updateChildren(updates).await()
                onSuccess()

            } catch (e: Exception) {
                onError("Errore chiusura: ${e.message}")
            }
        }
    }

    // ------------------------------------------------------------------
    // 6. ANNULLA PREPARAZIONE (PULIZIA LOCKER)
    // ------------------------------------------------------------------
    fun cancelReturnPreparation(rental: Rental) {
        viewModelScope.launch {
            try {
                val updates = hashMapOf<String, Any?>()

                // Rimuoviamo il rental e il codice dal locker fisico
                updates["/stations/${rental.stationId}/lockers/${rental.lockerId}/rental"] = null

                // (Opzionale) Rimuoviamo il riferimento del locker dal rental
                updates["/rentals/${rental.rentalId}/lockerId"] = null
                updates["/users/${auth.currentUser?.uid}/rentals/${rental.rentalId}/lockerId"] = null
                updates["/stations/${rental.stationId}/rentals/${rental.rentalId}/lockerId"] = null

                dbRef.updateChildren(updates).await()
                println("DEBUG: Annullamento preparazione completato per locker $")

            } catch (e: Exception) {
                println("ERRORE annullamento preparazione: ${e.message}")
            }
        }
    }

    // Helper per il calcolo costi
    private fun calculateSingleRentalCost(powerBankTypes: Map<String, Int>, minutes: Long): Double {
        val typeKey = powerBankTypes.keys.firstOrNull() ?: "BASIC"
        val strategy = when (typeKey) {
            "BASIC" -> PowerBank.BASIC
            "FAST" -> PowerBank.FAST
            "PRO" -> PowerBank.PRO
            else -> PowerBank.BASIC
        }
        var cost = minutes * strategy.pricePerMinute
        if (cost > strategy.maxDailyPrice) {
            cost = strategy.maxDailyPrice
        }
        return Math.round(cost * 100.0) / 100.0
    }

    // Mappa per tenere traccia di quali rental stiamo già ascoltando (per non mettere doppi listener)
    private val activeWatchers = mutableMapOf<String, com.google.firebase.database.ValueEventListener>()

    /**
     * Questa funzione va chiamata APPENA viene visualizzata una card.
     * Si aggancia al DB e reagisce ai cambiamenti di stato AUTONOMAMENTE.
     */


    fun watchRentalLifecycle(rentalId: String, stationId: String, lockerId: String) {
        if (activeWatchers.containsKey(rentalId)) return

        println("DEBUG: Inizio monitoraggio automatico per rental $rentalId")
        val ref = dbRef.child("stations").child(stationId)
            .child("lockers").child(lockerId)
            .child("rental")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val remoteRental = snapshot.getValue(Rental::class.java) ?: return

                println(
                    "DEBUG: ${remoteRental.state}"
                )
                val state = remoteRental.state

                // *** MODIFICA FONDAMENTALE QUI ***
                // Aggiorniamo la variabile che la UI sta ascoltando!
                _returnRentalState.value = state

                println("DEBUG: Stato rental cambiato in $state")
                // ********************************

                if (state == "ACTIVE") {
                    confirmPickup(remoteRental)
                }

                if (state == "COMPLETED" && (remoteRental.totalCost == null || remoteRental.totalCost == 0.0)) {
                    // AGGIUNGI UN DELAY QUI per permettere alla UI di reagire allo stato OPEN
                    viewModelScope.launch {
                        delay(3000) // Aspetta 3 secondi prima di eliminare tutto
                        terminateRental(
                            rental = remoteRental,
                            onSuccess = {

                                activeWatchers.remove(rentalId)
                                _returnRentalState.value = "COMPLETED"
                            },
                            onError = { println("ERRORE chiusura automatica: $it") }
                        )
                    }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }

        ref.addValueEventListener(listener)
        activeWatchers[rentalId] = listener
    }

    // Ricordati di pulire i listener quando il ViewModel muore (opzionale ma buona pratica)
    override fun onCleared() {
        super.onCleared()
        activeWatchers.forEach { (id, listener) ->
            dbRef.child("rentals").child(id).removeEventListener(listener)
        }
    }

    // Classe di appoggio
    data class TempLocker(val id: String, val type: String, val pbId: String?)
}

