package com.example.ricarica.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.ricarica.data.model.StationItem
import com.example.ricarica.firebase.observeStations
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

class HomeViewModel : ViewModel() {
    var stationList = mutableStateOf<List<StationItem>>(emptyList())
        private set
    private var ref: DatabaseReference? = null
    private var listener: ValueEventListener? = null

    init {
        val (r, l) = observeStations(

            onResult = { list ->
                // QUI arrivano i dati dal database
                stationList.value = list
            },
            onError = { error ->
                // per ora puoi anche ignorarlo o fare un log
                println("Firebase error: $error")
            }
        )

        ref = r
        listener = l
    }

    override fun onCleared() {
        // IMPORTANTISSIMO: stacchi il listener quando il VM muore
        if (ref != null && listener != null) {
            ref!!.removeEventListener(listener!!)
        }
        super.onCleared()
    }
}
