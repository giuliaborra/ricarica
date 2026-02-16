package NEW_VERSION

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

// Assicurati che il tuo sealed class abbia tutti gli elementi
sealed class BottomNavItem(var title: String, var iconFilled: ImageVector, var iconOutlined: ImageVector, var route: String) {
    object Map : BottomNavItem("Mappa", Icons.Filled.Map, Icons.Outlined.Map, "home")
    object Rentals : BottomNavItem("Noleggi", Icons.Filled.ElectricBolt, Icons.Outlined.ElectricBolt, "rentals_list")
    object History : BottomNavItem("Catalogo", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "catalog") // Aggiungi se manca
    object Profile : BottomNavItem("Profilo", Icons.Filled.Person, Icons.Outlined.Person, "profile")
}

@Composable
fun BottomNavBar(
    navController: NavController,
    isGuest: Boolean,            // <--- 1. NUOVO PARAMETRO
    onLoginRequest: () -> Unit   // <--- 2. NUOVO PARAMETRO (Callback per il login)
) {
    val items = listOf(
        BottomNavItem.Map,
        BottomNavItem.Rentals,
        BottomNavItem.History,
        BottomNavItem.Profile
    )

    // Stato per mostrare il popup
    var showGuestDialog by remember { mutableStateOf(false) }

    NavigationBar(
        containerColor = Color.White
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                        contentDescription = item.title
                    )
                },
                label = { Text(text = item.title) },
                selected = isSelected,
                onClick = {
                    // --- 3. LOGICA DI CONTROLLO ---
                    if (item == BottomNavItem.Profile && isGuest) {
                        // Se è Profilo ED è Ospite -> Mostra Dialog, NON navigare
                        showGuestDialog = true
                    } else {
                        // Comportamento standard: Naviga
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2E7D32),
                    selectedTextColor = Color(0xFF2E7D32),
                    indicatorColor = Color(0xFFE8F5E9),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }

    // --- 4. IL DIALOG POPUP ---
    if (showGuestDialog) {
        AlertDialog(
            onDismissRequest = { showGuestDialog = false },
            title = { Text("Accesso Richiesto") },
            text = { Text("Per visualizzare il tuo profilo, gestire i pagamenti e modificare la password devi accedere o registrarti.") },
            confirmButton = {
                Button(
                    onClick = {
                        showGuestDialog = false
                        onLoginRequest() // Porta alla pagina di Login
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Accedi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestDialog = false }) {
                    Text("Chiudi", color = Color.Gray)
                }
            },
            containerColor = Color.White
        )
    }
}