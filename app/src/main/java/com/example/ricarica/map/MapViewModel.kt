import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.ricarica.StationInfoCard
import com.example.ricarica.data.model.StationItem
import com.example.ricarica.rental.Rental

sealed interface MapSheetState {
    // Caso 1: Niente da mostrare
    object Hidden : MapSheetState

    // Caso 2: Timer prenotazione (Priorità massima)
    data class Reserved(val rentals: List<Rental>) : MapSheetState

    // Caso 3: Info Stazione (Priorità media - per nuovi noleggi)
    data class StationInfo(val station: StationItem) : MapSheetState

    // Caso 4: Lista noleggi attivi (Priorità bassa - persistente)
    data class ActiveRentals(val rentals: List<Rental>) : MapSheetState
}

class MapViewModel : ViewModel() {

    val selectedStation = mutableStateOf<StationItem?>(null)


    fun onMarkerClick(st: StationItem) {
        selectedStation.value = st
    }


    fun dismissStation() {
        selectedStation.value = null
    }

    fun computeSheetState(rentals: List<Rental>): MapSheetState {
        val currentStation = selectedStation.value
        val reservedList = rentals.filter { it.state == "RESERVED" }
        val activeList = rentals.filter { it.state == "ACTIVE" }

        return when {
            // 1. Il Timer di prenotazione vince su tutto
            reservedList.isNotEmpty() -> MapSheetState.Reserved(reservedList)

            // 2. Se non c'è prenotazione, ma ho cliccato una stazione, mostro quella
            currentStation != null -> MapSheetState.StationInfo(currentStation)

            // 3. Se non sto facendo nulla, ma ho noleggi attivi, mostro la lista ridotta
            activeList.isNotEmpty() -> MapSheetState.ActiveRentals(activeList)

            // 4. Altrimenti nascondo tutto
            else -> MapSheetState.Hidden
        }
    }

}

