import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ricarica.profile.AuthViewModel

@Composable
fun LoginPage(
    authViewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column {
        Text("Login")

        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
        TextField(value = password, onValueChange = { password = it }, label = { Text("Password") })

        if (error != null) Text(error!!)

        Button(onClick = {
            error = null
            authViewModel.login(
                email = email,
                password = password,
                onSuccess = onSuccess,
                onError = { msg -> error = msg }
            )
        }) {
            Text("Accedi")
        }

        TextButton(onClick = onBack) { Text("Indietro") }
    }
}
