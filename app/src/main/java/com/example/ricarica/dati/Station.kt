package com.example.ricarica.data.model
import android.location.Address
import com.example.ricarica.dati.Locker
import com.example.ricarica.rental.Rental

data class StationItem(
    val id: String,
    val station: Station
)

data class Station(
    val enabled: Boolean = true,
    val stationId: String = "",
    val name: String = "",
    val address: String = "",
    val position: Position? = null,
    val lockers: Map<String, Locker> = emptyMap(),
    val rentals: Map<String, Rental> = emptyMap()

)

data class Position (
    val lat: Double = 0.0,
    val lng: Double = 0.0,

)





