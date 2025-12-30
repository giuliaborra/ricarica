// In un file come data/model/PowerBank.kt

/**
 * Sealed class che rappresenta i diversi tipi di Power Bank.
 * Ogni sottoclasse definisce le proprie caratteristiche specifiche.
 */
sealed class PowerBank(
    val title: String,
    val features: List<String>
) {
    // Sottoclasse per il modello Basic
    data object Basic : PowerBank(
        title = "Power Bank Basic",
        features = listOf(
            "Ricarica standard (5W)",
            "1 porta USB-A",
            "Indicatore LED di stato"
        )
    )

    // Sottoclasse per il modello Fast
    data object Fast : PowerBank(
        title = "Power Bank Fast",
        features = listOf(
            "Ricarica Rapida (18W Power Delivery)",
            "1 porta USB-C (In/Out)",
            "1 porta USB-A Quick Charge 3.0",
            "Design compatto"
        )
    )

    // Sottoclasse per il modello Pro
    data object Pro : PowerBank(
        title = "Power Bank Pro",
        features = listOf(
            "Ricarica Ultra-Rapida (65W Power Delivery)",
            "Ideale per laptop e tablet",
            "2 porte USB-C (In/Out)",
            "1 porta USB-A",
            "Display digitale per percentuale esatta"
        )
    )
}
