package NEW_VERSION

import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(var title: String, var iconFilled: ImageVector, var iconOutlined: ImageVector, var route: String) {
    object Map : BottomNavItem("Mappa", Icons.Filled.Map, Icons.Outlined.Map, "home")
    object Rentals : BottomNavItem("Noleggi", Icons.Filled.ElectricBolt, Icons.Outlined.ElectricBolt, "rentals_list")
    object History : BottomNavItem("Catalogo", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "catalog")
    object Profile : BottomNavItem("Profilo", Icons.Filled.Person, Icons.Outlined.Person, "profile")
}

@OptIn(ExperimentalMaterial3Api::class) // Necessario per BadgedBox
@Composable
fun BottomNavBar(
    navController: NavController,
    isGuest: Boolean,
    activeRentalsCount: Int, // <--- 1. NUOVO PARAMETRO (Numero noleggi attivi)
    onLoginRequest: () -> Unit
) {
    val items = listOf(
        BottomNavItem.Map,
        BottomNavItem.Rentals,
        BottomNavItem.History,
        BottomNavItem.Profile
    )

    var showGuestDialog by remember { mutableStateOf(false) }

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                icon = {
                    // --- 2. LOGICA DEL BADGE ---
                    // Se l'item è "Noleggi" E ci sono noleggi attivi (>0), mostriamo il pallino
                    if (item == BottomNavItem.Rentals && activeRentalsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(

                                    modifier = Modifier.offset(x = 6.dp, y = (-4).dp),
                                    containerColor = Color(0xFFD32F2F), // Rosso
                                    contentColor = Color.White
                                ) {
                                    Text("$activeRentalsCount")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                                contentDescription = item.title
                            )
                        }
                    } else {
                        // Icona normale senza badge
                        Icon(
                            imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                            contentDescription = item.title
                        )
                    }
                },
                label = { Text(text = item.title) },
                selected = isSelected,
                onClick = {
                    if (item == BottomNavItem.Profile && isGuest) {
                        showGuestDialog = true
                    } else {
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

    if (showGuestDialog) {
        AlertDialog(
            onDismissRequest = { showGuestDialog = false },
            title = { Text("Accesso Richiesto") },
            text = { Text("Per visualizzare il tuo profilo devi accedere.") },
            confirmButton = {
                Button(
                    onClick = {
                        showGuestDialog = false
                        onLoginRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Accedi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestDialog = false }) { Text("Chiudi", color = Color.Gray) }
            },
            containerColor = Color.White
        )
    }
}