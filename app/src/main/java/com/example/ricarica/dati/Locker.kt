package com.example.ricarica.dati

data class Locker(
    val lastChangeAt: Long = 0L,
    val occupied: Boolean = false,
    val powerBankId: String = "",
    val state: String = ""
)