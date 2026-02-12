package com.example.ricarica.rental

import PowerBank
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.ceil

class RentalViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val dbRef = FirebaseDatabase.getInstance().reference

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
                        childUpdates["/stations/${stationId}/lockers/${chosenLocker.id}/unlock_code"] = passkey
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

            //tempo di inizio



            // Ripristiniamo il locker
            snapshot.children.forEach { lockerSnapshot ->
                val lockerId = lockerSnapshot.key
                val currentPassKeyLong = lockerSnapshot.child("unlock_code").getValue(Long::class.java)
                val currentPassKey = currentPassKeyLong?.toInt()

                if (currentPassKey == targetPasskey) {
                    // Torna OCCUPIED perché il powerbank è ancora lì
                    childUpdates["/stations/${rental.stationId}/lockers/$lockerId/state"] = "OCCUPIED"
                    childUpdates["/stations/${rental.stationId}/lockers/$lockerId/unlock_code"] = null
                }
            }

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
                updates["/rentals/${rental.rentalId}/state"] = "ACTIVE"
                updates["/users/${currentUser.uid}/rentals/${rental.rentalId}/state"] = "ACTIVE"
                updates["/stations/$stationId/rentals/${rental.rentalId}/state"] = "ACTIVE"

                //aggiorna tempo
                updates["/rentals/${rental.rentalId}/startTime"] = now
                updates["/users/${currentUser.uid}/rentals/${rental.rentalId}/startTime"] = now
                updates["/users/${currentUser.uid}/rentals/${rental.rentalId}/startTime"] = now




                // Libera il locker (l'utente ha preso la batteria)
                val lockersRef = dbRef.child("stations").child(stationId).child("lockers")
                val snapshot = lockersRef.get().await()

                for (lockerSnapshot in snapshot.children) {
                    val storedPasskeyLong = lockerSnapshot.child("unlock_code").getValue(Long::class.java)
                    val storedPasskey = storedPasskeyLong?.toInt()
                    val lockerId = lockerSnapshot.key

                    if (storedPasskey == rental.unlock_code && lockerId != null) {
                        updates["/stations/$stationId/lockers/$lockerId/state"] = "FREE"
                        updates["/stations/$stationId/lockers/$lockerId/unlock_code"] = null
                        updates["/stations/$stationId/lockers/$lockerId/powerBankId"] = null
                        updates["/stations/$stationId/lockers/$lockerId/type"] = null
                        updates["/stations/$stationId/lockers/$lockerId/rental"] = null
                    }
                }
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
        onReadyToPlay: (Int, String) -> Unit, // Restituisce (CodiceDaSuonare, IDLocker)
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // VINCOLO: Restituzione nella STESSA stazione
                val stationId = rental.stationId
                val lockersRef = dbRef.child("stations").child(stationId).child("lockers")
                val lockersSnapshot = lockersRef.get().await()

                // 1. Cerca il primo locker tecnicamente LIBERO
                var targetLockerId: String? = null
                for (locker in lockersSnapshot.children) {
                    val state = locker.child("state").getValue(String::class.java)
                    if (state == "FREE") {
                        targetLockerId = locker.key
                        break
                    }
                }

                if (targetLockerId == null) {
                    onError("Nessuno slot libero in questa stazione per la restituzione.")
                    return@launch
                }

                // 2. Genera codice per la restituzione
                val returnCode = (1000..9999).random()

                // 3. ISTRUISCI IL LOCKER SPECIFICO
                // Scriviamo nel locker target: "Aspettati questo codice e questo noleggio"
                val updates = hashMapOf<String, Any?>()

                // Il locker leggerà "unlock_code". Se il microfono sente questo numero -> APRE.
                updates["/stations/$stationId/lockers/$targetLockerId/unlock_code"] = returnCode

                // Salviamo l'oggetto Rental aggiornato nel locker (per sicurezza e logica hardware)
                val rentalForLocker = rental.copy(unlock_code = returnCode)
                updates["/stations/$stationId/lockers/$targetLockerId/rental"] = rentalForLocker

                dbRef.updateChildren(updates).await()

                // 4. Diamo l'OK alla UI per suonare
                onReadyToPlay(returnCode, targetLockerId)

            } catch (e: Exception) {
                onError("Errore preparazione restituzione: ${e.message}")
            }
        }
    }

    // ------------------------------------------------------------------
    // 5. CHIUSURA NOLEGGIO (COMPLETED)
    // ------------------------------------------------------------------
    fun terminateRental(
        rental: Rental,
        targetLockerId: String, // Locker dove è stato restituito
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
                updates["/stations/$stationId/lockers/$targetLockerId/state"] = "OCCUPIED"
                updates["/stations/$stationId/lockers/$targetLockerId/powerBankId"] = rental.powerBankId
                updates["/stations/$stationId/lockers/$targetLockerId/type"] = rental.type

                // PULIZIA LOCKER (Rimuovi codice e rental temporaneo)
                updates["/stations/$stationId/lockers/$targetLockerId/unlock_code"] = null
                updates["/stations/$stationId/lockers/$targetLockerId/rental"] = null

                dbRef.updateChildren(updates).await()
                onSuccess()

            } catch (e: Exception) {
                onError("Errore chiusura: ${e.message}")
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

    // Classe di appoggio
    data class TempLocker(val id: String, val type: String, val pbId: String?)
}