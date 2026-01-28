package com.example.ricarica.home
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.ricarica.data.model.StationItem
import com.example.ricarica.firebase.observeStations
import com.example.ricarica.rental.Rental
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeViewModel : ViewModel() {
    // Lista delle stazioni sulla mappa
    var stationList = mutableStateOf<List<StationItem>>(emptyList())
        private set

    // MODIFICA 1: Invece di un singolo 'activeRental', ora usiamo una LISTA
    val userRentals = mutableStateOf<List<Rental>>(emptyList())

    // Riferimenti per la pulizia dei dati
    private var stationsRef: DatabaseReference? = null
    private var stationsListener: ValueEventListener? = null
    private var rentalRef: DatabaseReference? = null
    private var rentalListener: ValueEventListener? = null

    init {
        // 1. Iniziamo a osservare le stazioni
        val (r, l) = observeStations(
            onResult = { list -> stationList.value = list },
            onError = { println("Firebase error") }
        )
        stationsRef = r
        stationsListener = l

        // 2. Avviamo l'osservazione dei noleggi dell'utente
        observeUserRental()
    }

    private fun observeUserRental() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Puntiamo al nodo dei noleggi PERSONALI dell'utente
        rentalRef = FirebaseDatabase.getInstance().getReference("users/$userId/rentals")

        rentalListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // MODIFICA 2: Mappiamo tutti i figli in una lista
                val foundRentals = snapshot.children.mapNotNull {
                    it.getValue(Rental::class.java)
                }.filter {
                    // MODIFICA 3: Filtriamo solo quelli interessanti (Prenotati o Attivi)
                    // Escludiamo quelli terminati/vecchi per non intasare la UI
                    it.state == "RESERVED" || it.state == "ACTIVE"
                }

                // Aggiorniamo la lista osservata dalla UI
                userRentals.value = foundRentals

                println("DEBUG: Noleggi attivi trovati: ${foundRentals.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                println("Errore lettura noleggi: ${error.message}")
            }
        }

        rentalRef?.addValueEventListener(rentalListener!!)
    }

    override fun onCleared() {
        stationsRef?.removeEventListener(stationsListener!!)
        rentalRef?.removeEventListener(rentalListener!!)
        super.onCleared()
    }
}