package com.example.ricarica.firebase
import com.example.ricarica.data.model.StationItem
import com.example.ricarica.data.model.Station
import com.google.firebase.database.*

fun observeStations (
    onResult: (List<StationItem>) -> Unit,
    onError: (String) -> Unit
): Pair<DatabaseReference, ValueEventListener> {


    //mi collego al nodo station del mio database
    val ref = FirebaseDatabase.getInstance().getReference("stations")

    // osservatore, viene chiamato ogni volta che stations cambia
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val stations = snapshot.children.mapNotNull { stSnap ->
                //in ID mi salva la chiave del mio nodo quindi st_01 / st_02 ecc...
                val id = stSnap.key ?: return@mapNotNull null
                val station = stSnap.getValue(Station::class.java) ?: return@mapNotNull null
                StationItem(id = id, station = station)
            }
            onResult(stations)
        }

        override fun onCancelled(error: DatabaseError) {
            onError(error.message)
        }
    }

    ref.addValueEventListener(listener)
    return ref to listener
}
