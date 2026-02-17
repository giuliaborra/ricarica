package com.example.ricarica.rental

enum class RentalStatus {
    RESERVED, // Prenotato (i 20 minuti)
    ACTIVE,   // Powerbank prelevato
    COMPLETED // Restituito
}

data class Rental(
    val rentalId: String = "",
    val userId: String = "",
    val stationId: String = "",
    val powerBankId: String? = "",
    var lockerId: String = "",
    val type: String = "",
    val totalCost: Double?=null,
    // Esempio nel DB: { "Basic": 1, "Fast": 2 }
    val powerBankTypes: Map<String, Int> = emptyMap(),
    val unlock_code: Int? = null,
    val state: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L
)