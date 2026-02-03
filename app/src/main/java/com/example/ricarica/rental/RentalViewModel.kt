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

    // ... createRental rimane uguale (assicurati solo che scriva "unlock_code" ovunque) ...
    // Riporto createRental per sicurezza sui nomi dei campi
    // In RentalViewModel.kt

    fun createRental(
        stationId: String, // Passiamo l'ID
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

        // Mappatura selezione
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
                // 1. SCARICA I LOCKER DAL DB (Fix per locker vuoti)
                val lockersRef = dbRef.child("stations").child(stationId).child("lockers")
                val snapshot = lockersRef.get().await()

                // Crea una lista temporanea di locker disponibili
                val availableLockers = snapshot.children.mapNotNull { doc ->
                    val state = doc.child("state").getValue(String::class.java)
                    val type = doc.child("type").getValue(String::class.java)
                    val lockerId = doc.key
                    val pbId = doc.child("powerBankId").getValue(String::class.java)

                    if (state == "OCCUPIED" && type != null && lockerId != null) {
                        // Oggetto temporaneo leggero
                        TempLocker(lockerId, type, pbId)
                    } else null
                }.toMutableList()

                val now = System.currentTimeMillis()
                val expiryTime = now + (20 * 60 * 1000)
                val childUpdates = hashMapOf<String, Any>()
                val createdRentals = mutableListOf<Rental>()

                // 2. CICLO DI ASSEGNAZIONE
                for ((type, quantity) in requestMap) {

                    val lockersForType = availableLockers.filter { it.type == type }

                    if (lockersForType.size < quantity) {
                        onError("Non ci sono abbastanza powerbank di tipo $type.")
                        return@launch
                    }

                    repeat(quantity) {
                        val chosenLocker = lockersForType.first()
                        availableLockers.remove(chosenLocker) // Rimuovi per non riusarlo

                        val rentalKey = dbRef.child("rentals").push().key ?: return@launch
                        val passkey = (1000..9999).random() // Genera codice INT

                        // 3. CREA L'OGGETTO COMPLETO QUI (Fix Ancestor Error)
                        // Inserisci TUTTI i dati qui dentro, non aggiornarli dopo.
                        val newRental = Rental(
                            rentalId = rentalKey,
                            userId = currentUser.uid,
                            stationId = stationId,
                            powerBankTypes = mapOf(type to 1),
                            unlock_code = passkey, // Campo INT corretto
                            state = "RESERVED",
                            startTime = now,
                            endTime = expiryTime,

                            // DATI AGGIUNTI DIRETTAMENTE NELL'OGGETTO
                            powerBankId = chosenLocker.pbId,
                            type = type,
                            lockerId = chosenLocker.id // O assignedLockerId se la tua classe lo chiama così
                        )

                        createdRentals.add(newRental)

                        // 4. PREPARA GLI AGGIORNAMENTI (Senza conflitti)

                        // Scrivi l'oggetto Rental completo
                        childUpdates["/rentals/$rentalKey"] = newRental
                        childUpdates["/users/${currentUser.uid}/rentals/$rentalKey"] = newRental
                        childUpdates["/stations/${stationId}/rentals/$rentalKey"] = newRental

                        // *** NOTA: HO RIMOSSO LE RIGHE CHE CRASHAVANO QUI ***
                        // Non serve scrivere powerBankId separatamente perché è già dentro 'newRental'

                        // Aggiorna lo stato del Locker fisico
                        childUpdates["/stations/${stationId}/lockers/${chosenLocker.id}/state"] = "RESERVED"
                        childUpdates["/stations/${stationId}/lockers/${chosenLocker.id}/unlock_code"] = passkey
                    }
                }

                // 5. SCRIVI TUTTO SU FIREBASE
                dbRef.updateChildren(childUpdates).await()
                onSuccess(createdRentals)

            } catch (e: Exception) {
                onError("Errore creazione: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Classe di appoggio (mettila fuori dalla classe o in fondo al file)
    data class TempLocker(val id: String, val type: String, val pbId: String?)

    // --- FUNZIONI CORRETTE PER GESTIRE INT ---

    fun deleteReserved(rental: Rental?) {
        if (rental == null) return

        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            // Qui rental.unlock_code è già INT
            val targetPasskey = rental.unlock_code

            val stationRef = dbRef.child("stations").child(rental.stationId).child("lockers")
            val snapshot = stationRef.get().await()
            val childUpdates = hashMapOf<String, Any?>()

            childUpdates["/rentals/${rental.rentalId}"] = null
            childUpdates["/users/${currentUser.uid}/rentals/${rental.rentalId}"] = null
            childUpdates["/stations/${rental.stationId}/rentals/${rental.rentalId}"] = null

            snapshot.children.forEach { lockerSnapshot ->
                val lockerId = lockerSnapshot.key

                // FIX 1: Leggiamo "unlock_code" (c'era un typo "unlock_cos")
                // FIX 2: Leggiamo come Long e convertiamo a Int (per evitare crash Long->String)
                val currentPassKeyLong = lockerSnapshot.child("unlock_code").getValue(Long::class.java)
                val currentPassKey = currentPassKeyLong?.toInt()

                // FIX 3: Confronto tra Int e Int (c'era un assegnazione =)
                if (currentPassKey == targetPasskey) {
                    childUpdates["/stations/${rental.stationId}/lockers/$lockerId/state"] = "OCCUPIED"
                    childUpdates["/stations/${rental.stationId}/lockers/$lockerId/unlock_code"] = null
                }
            }

            dbRef.updateChildren(childUpdates).addOnSuccessListener {
                println("Noleggio cancellato")
            }
        }
    }

    fun confirmPickup(rental: Rental) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val stationId = rental.stationId
                val updates = hashMapOf<String, Any?>()

                updates["/rentals/${rental.rentalId}/state"] = "ACTIVE"
                updates["/users/${currentUser.uid}/rentals/${rental.rentalId}/state"] = "ACTIVE"
                updates["/stations/$stationId/rentals/${rental.rentalId}/state"] = "ACTIVE"

                val lockersRef = dbRef.child("stations").child(stationId).child("lockers")
                val snapshot = lockersRef.get().await()

                for (lockerSnapshot in snapshot.children) {
                    // FIX: Chiave coerente "unlock_code" e lettura come Long -> Int
                    val storedPasskeyLong = lockerSnapshot.child("unlock_code").getValue(Long::class.java)
                    val storedPasskey = storedPasskeyLong?.toInt()

                    val lockerId = lockerSnapshot.key

                    // Confronto Int == Int
                    if (storedPasskey == rental.unlock_code && lockerId != null) {
                        updates["/stations/$stationId/lockers/$lockerId/state"] = "FREE"
                        updates["/stations/$stationId/lockers/$lockerId/unlock_code"] = null
                        updates["/stations/$stationId/lockers/$lockerId/powerBankId"] = null
                        updates["/stations/$stationId/lockers/$lockerId/type"] = null
                    }
                }
                dbRef.updateChildren(updates).await()
            } catch (e: Exception) {
                println("Errore conferma ritiro: ${e.message}")
            }
        }
    }

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

                val stationRentalRef = dbRef.child("stations").child(stationId).child("rentals").child(rentalId)
                val lockersRef = dbRef.child("stations").child(stationId).child("lockers")

                val rentalSnapshot = stationRentalRef.get().await()
                val lockersSnapshot = lockersRef.get().await()



                if (!rentalSnapshot.exists()) {
                    onError("Errore: Ordine non trovato in questa stazione.")
                    return@launch
                }

                // *** FIX CRUCIALE PER IL TUO ERRORE ***
                // 1. Leggiamo dal DB come Long (perché Firebase salva i numeri così)
                val storedPasskeyLong = rentalSnapshot.child("unlock_code").getValue(Long::class.java)

                // 2. Convertiamo in Int (o null se non esiste)
                val storedPasskey = storedPasskeyLong?.toInt()

                // 3. Ora rental.unlock_code (Int) e storedPasskey (Int?) sono compatibili
                if (storedPasskey != rental.unlock_code) {
                    onError("Codice di sicurezza non corrispondente.")
                    return@launch
                }

                // Cerca locker libero
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

                val now = System.currentTimeMillis()
                val updates = hashMapOf<String, Any?>()

                // 1. Calcolo Durata in Minuti
                val durationMillis = now - rental.startTime
                // Arrotondiamo sempre per eccesso (es. 61 secondi = 2 minuti)
                val durationMinutes = ceil(durationMillis / 60000.0).toLong()

                // 2. Calcolo Costo Totale (Semplificato)
                val totalCost = calculateSingleRentalCost(rental.powerBankTypes, durationMinutes)

                //MODIFICHE RENTALS
                updates["/rentals/$rentalId/state"] = "COMPLETED"
                updates["/rentals/$rentalId/endTime"] = now
                updates["rentals/$rentalId/unlock_code"] = null
                updates["rentals/$rentalId/totalCost"] = totalCost

                //MODIFICHE IN USER
                updates["/users/${currentUser.uid}/rentals/$rentalId/state"] = "COMPLETED"
                updates["/users/${currentUser.uid}/rentals/$rentalId/endTime"] = now
                updates["users/${currentUser.uid}/rentals/${rentalId}/unlock_code"] = null
                updates["users/${currentUser.uid}/rentals/${rentalId}/totalCost"] = null

                //MODIFICHE IN STATION
                updates["/stations/$stationId/rentals/$rentalId/state"] = "COMPLETED"
                updates["/stations/$stationId/rentals/$rentalId/endTime"] = now
                updates["/stations/$stationId/rentals/$rentalId/unlock_code"] = null
                updates["/stations/$stationId/rentals/$rentalId/totalCost"] = null

                //MODIFICHE LOCKERS FINALE
                updates["/stations/$stationId/lockers/$targetLockerId/state"] = "OCCUPIED"
                updates["/stations/$stationId/lockers/$targetLockerId/powerBankId"] = rental.powerBankId
                updates["/stations/$stationId/lockers/$targetLockerId/type"] = rental.type


                dbRef.updateChildren(updates).await()
                onSuccess()

            } catch (e: Exception) {
                onError("Errore durante la restituzione: ${e.message}")
            }
        }
    }


}

private fun calculateSingleRentalCost(powerBankTypes: Map<String, Int>, minutes: Long): Double {

    val typeKey = powerBankTypes.keys.firstOrNull() ?: "BASIC"

    val strategy = when (typeKey) {
        "BASIC" -> PowerBank.BASIC
        "FAST" -> PowerBank.FAST
        "PRO" -> PowerBank.PRO
        else -> PowerBank.BASIC
    }

    // Calcolo: Minuti * Prezzo al Minuto
    var cost = minutes * strategy.pricePerMinute

    // Tetto massimo giornaliero
    if (cost > strategy.maxDailyPrice) {
        cost = strategy.maxDailyPrice
    }

    // Arrotondamento a 2 decimali (es. 1.234 -> 1.23)
    return Math.round(cost * 100.0) / 100.0
}


