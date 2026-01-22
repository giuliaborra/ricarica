package com.example.ricarica.dati

import com.example.ricarica.rental.Rental

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val createdAt: Long = 0L,
    val userName: String = "",
    val rentals: Map<String, Rental> = emptyMap()
)