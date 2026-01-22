import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ricarica.rental.Rental
import com.example.ricarica.profile.AuthViewModel

@Composable
fun RegisterPage(
    authViewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var rentals by remember { mutableStateOf(emptyMap<String,Rental>()) }
    var error by remember { mutableStateOf<String?>(null) }


    Column {
        Text("Registrazione")

        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
        TextField(value = username, onValueChange = { username = it }, label = { Text("Username") })

        if (error != null) Text(error!!)

        Button(onClick = {
            error = null
            authViewModel.register(
                email = email,
                password = password,
                username = username,
                rentals = rentals,
                onSuccess = onSuccess,
                onError = { msg -> error = msg }
            )
        }) {
            Text("Registrati")
        }

        TextButton(onClick = onBack) { Text("Indietro") }
    }
}
