package com.example.ricarica.dati

data class CartItem(
    val powerBankId: String = "",
    val quantity: Int = 1,
    val addedAt: Long = 0L
)

data class Cart(
    val expiresAt: Long = 0L,
    val items: Map<String, CartItem> = emptyMap()
)
