// File: data/model/PowerBank.kt

sealed class PowerBank(
    val title: String,
    val features: List<String>,
    val pricePerMinute: Double, // Prezzo al minuto
    val deposit: Double,        // Cauzione
    val maxDailyPrice: Double   // Prezzo massimo per 24h (opzionale ma consigliato)
) {
    // 1. BASIC: Economico, per chi non ha fretta
    data object BASIC : PowerBank(
        title = "Power Bank Basic",
        features = listOf(
            "Ricarica standard (5W)",
            "1 porta USB-A",
            "Ideale per smartphone"
        ),
        pricePerMinute = 0.01, // 60 cent all'ora
        deposit = 10.00,
        maxDailyPrice = 5.00
    )

    // 2. FAST: Il più popolare, ricarica rapida
    data object FAST : PowerBank(
        title = "Power Bank Fast",
        features = listOf(
            "Ricarica Rapida (18W)",
            "USB-C + USB-A",
            "50% di batteria in 30 min"
        ),
        pricePerMinute = 0.03, // 1.80€ all'ora
        deposit = 20.00,
        maxDailyPrice = 10.00
    )

    // 3. PRO: Premium, per chi deve lavorare col PC
    data object PRO : PowerBank(
        title = "Power Bank Pro",
        features = listOf(
            "Ultra-Rapida (65W)",
            "Carica anche Laptop/MacBook",
            "Display digitale LCD"
        ),
        pricePerMinute = 0.05, // 3.00€ all'ora
        deposit = 50.00,
        maxDailyPrice = 15.00
    )
}