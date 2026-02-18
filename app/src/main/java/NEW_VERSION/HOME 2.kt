package NEW_VERSION
import MapViewModel
import com.example.ricarica.home.HomeViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import NEW_VERSION.profile.AuthViewModel
import com.example.ricarica.rental.RentalViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.Dp
import com.example.ricarica.map.MapViewModern

//import com.example.ricarica.map.MapViewModern

@Composable
fun HomePageNew(
    viewModel: HomeViewModel,
    navController: NavController,
    isGuest: Boolean,
    bottomPadding: Dp
) {
    val mapVm: MapViewModel = viewModel()
    val rentalVm: RentalViewModel = viewModel()
    val authVm: AuthViewModel = viewModel()

    val stations = viewModel.stationList.value
    val rentals = authVm.activeRentals.collectAsState().value


        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomPadding)

        ) {
            MapViewModern (
                vm = mapVm,
                rentals = rentals,
                stations = stations,
                onConfirmRental = { rental -> rentalVm.confirmPickup(rental) },
                onDeleteReserved = { rental -> rentalVm.deleteReserved(rental)  },
                onLoginRequest = { navController.navigate("login")},
                isGuest = isGuest,
                rentalViewModel = rentalVm





            )
        }
}


