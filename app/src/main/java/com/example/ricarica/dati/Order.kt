package com.example.ricarica.dati

data class ActiveOrder(
    val orderId: String = "",
    val items: Map<String, CartItem> = emptyMap(),
    val createdAt: Long = 0L,
    val status: String = "in_rental"
)


data class PastOrder(
    val orderId: String = "",
    val items: Map<String, CartItem> = emptyMap(),
    val createdAt: Long = 0L,
    val completedAt: Long = 0L,
    val totalPrice: Double = 0.0
)
