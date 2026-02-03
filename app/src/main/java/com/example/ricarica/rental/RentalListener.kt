
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

fun watchRentalStatus(rentalId: String, onStateActive: () -> Unit) {
    val dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().reference

    dbRef.child("rentals").child(rentalId).child("state")
        .addValueEventListener(object : ValueEventListener { // <--- Controlla questa riga
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(String::class.java)

                if (state == "ACTIVE") {
                    onStateActive()
                    // Rimuoviamo il listener per evitare che continui a girare
                    dbRef.child("rentals").child(rentalId).child("state").removeEventListener(this)
                }


            }

            override fun onCancelled(error: DatabaseError) {
                // Gestisci l'errore se necessario
            }
        })

    fun watchReturnStatus(rentalId: String, onCompleted: () -> Unit) {
        // Riferimento al database per lo stato di questo specifico noleggio
        val statusRef = dbRef.child("rentals").child(rentalId).child("state")

        statusRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val state = snapshot.getValue(String::class.java)

                // Quando Arduino rileva il PowerBank e scrive COMPLETED sul DB
                if (state == "COMPLETED") {
                    // Eseguiamo l'azione di successo (es. mostrare la ricevuta)
                    onCompleted()

                    // IMPORTANTE: Rimuoviamo il listener per non sprecare risorse
                    statusRef.removeEventListener(this)
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                println("Errore durante l'ascolto della restituzione: ${error.message}")
            }
        })
    }

    fun watchStatus(rentalId: String, targetState: String, onTargetReached: () -> Unit) {
        dbRef.child("rentals").child(rentalId).child("state")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    if (s.getValue(String::class.java) == targetState) {
                        onTargetReached()
                        dbRef.child("rentals").child(rentalId).child("state").removeEventListener(this)
                    }
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

// E poi le usi così:
// watchStatus(id, "ACTIVE") { ... }  <-- per il prelievo
// watchStatus(id, "COMPLETED") { ... } <-- per la restituzione
}

