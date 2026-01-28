package com.example.ricarica.rental

import com.example.ricarica.data.model.Station
import com.example.ricarica.data.model.StationItem

enum class RentalStatus {
    RESERVED, // Prenotato (i 20 minuti)
    ACTIVE,   // Powerbank prelevato
    COMPLETED // Restituito
}

data class Rental(
    val rentalId: String = "",
    val userId: String = "",
    val stationId: String = "",
    // Esempio nel DB: { "Basic": 1, "Fast": 2 }
    val powerBankTypes: Map<String, Int> = emptyMap(),
    val passkey: String = "",
    val state: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L
)