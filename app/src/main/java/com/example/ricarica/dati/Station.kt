package com.example.ricarica.data.model
import com.example.ricarica.dati.Locker

data class StationItem(
    val id: String,
    val station: Station
)

data class Station(
    val enabled: Boolean = true,
    val name: String = "",
    val position: Position? = null,
    val lockers: Map<String, Locker> = emptyMap()
)

data class Position (
    val lat: Double = 0.0,
    val lng: Double = 0.0,

)





