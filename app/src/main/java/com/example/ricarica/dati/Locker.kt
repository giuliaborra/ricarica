package com.example.ricarica.dati

import android.R

object LockerStatus {
    const val FREE = "FREE"           // VUOTO: Disponibile per la RESTITUZIONE
    const val OCCUPIED = "OCCUPIED"   // PIENO: Disponibile per il NOLEGGIO
    const val RESERVED = "RESERVED"   // PRENOTATO: PB dentro, in attesa di ritiro (Timer 20min)
    const val BLOCKED = "BLOCKED"     // GUASTO
}
data class Locker(
    val lockerId: String = "",
    val powerBankId: String? = null,
    val type: String? = null,
    val state: String = "",
    val passKey: String? = null
)