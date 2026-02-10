package NEW_VERSION
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

// Definizione degli elementi della barra
sealed class BottomNavItem(var title: String, var iconFilled: ImageVector, var iconOutlined: ImageVector, var route: String) {
    object Map : BottomNavItem("Mappa", Icons.Filled.Map, Icons.Outlined.Map, "home")
    object Rentals : BottomNavItem("Noleggi", Icons.Filled.ElectricBolt, Icons.Outlined.ElectricBolt, "rentals_list") // Rotta ipotetica per la lista noleggi
    object Catalog : BottomNavItem("Catalogo", Icons.Filled.ViewList, Icons.Outlined.ViewList, "catalog")
    object Profile : BottomNavItem("Profilo", Icons.Filled.Person, Icons.Outlined.Person, "profile")
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Map,
        BottomNavItem.Rentals, // Manteniamo Rentals come da screenshot
        BottomNavItem.Catalog,
        BottomNavItem.Profile
    )

    NavigationBar(
        containerColor = Color.White, // Sfondo bianco come nello screenshot
        contentColor = Color.Black,
        tonalElevation = 8.dp // Leggera ombra
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
                    // Evita di ricaricare la stessa pagina se ci sei già
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Pulisce lo stack per evitare accumulo di schermate
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF2E7D32), // Verde scuro (stile PowerShare)
                    selectedTextColor = Color(0xFF2E7D32),
                    indicatorColor = Color(0xFFE8F5E9), // Sfondo verdino chiaro selezione
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}