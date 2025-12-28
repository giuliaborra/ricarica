import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.ricarica.StationInfoCard
import com.example.ricarica.data.model.StationItem

class MapViewModel : ViewModel() {

    val selectedStation = mutableStateOf<StationItem?>(null)


    fun onMarkerClick(st: StationItem) {
        selectedStation.value = st
    }


    fun dismissStation() {
        selectedStation.value = null
    }

}

