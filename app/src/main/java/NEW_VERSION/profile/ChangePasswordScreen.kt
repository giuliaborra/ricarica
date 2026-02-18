package NEW_VERSION.profile
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// --- COLORI RICA RICA ---
private val PowerGreen = Color(0xFF2E7D32)
private val TextBlack = Color(0xFF1A1A1A)
private val TextGray = Color(0xFF757575)
private val ErrorColor = Color(0xFFB00020)
private val BackgroundWhite = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onPasswordUpdated: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Stati dei campi
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Stati UI
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(

                title = {
                    Text(
                        "MODIFICA PASSWORD",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextBlack
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Indietro",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                ),
                modifier = Modifier.shadow(2.dp)
            )
        },
        containerColor = BackgroundWhite
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- HEADER VISIVO ---
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PowerGreen.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PowerGreen,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sicurezza Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            Text(
                text = "Scegli una password sicura per proteggere il tuo account Rica Rica.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- CAMPI INPUT ---

            RicaPasswordField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = "Password Attuale",
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(16.dp))

            RicaPasswordField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = "Nuova Password",
                imeAction = ImeAction.Next
            )

            Spacer(modifier = Modifier.height(16.dp))

            RicaPasswordField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Conferma Nuova Password",
                imeAction = ImeAction.Done,
                isError = newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword
            )

            // Messaggio errore validazione live
            if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                Text(
                    text = "Le password non coincidono",
                    color = ErrorColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, start = 8.dp)
                )
            }

            // Messaggio errore generale (dal server o logica)
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = ErrorColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- BOTTONE AZIONE ---
            Button(
                onClick = {
                    // Validazione Base
                    if (currentPassword.isBlank() || newPassword.isBlank()) {
                        errorMessage = "Compila tutti i campi."
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        errorMessage = "Le nuove password non coincidono."
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        errorMessage = "La password deve avere almeno 6 caratteri."
                        return@Button
                    }

                    // --- CHIAMATA AL VIEWMODEL ---
                    isLoading = true
                    errorMessage = null

                    authViewModel.updatePassword(
                        currentPass = currentPassword,
                        newPass = newPassword,
                        onSuccess = {
                            isLoading = false
                            showSuccessDialog = true // Mostra il popup verde
                        },
                        onError = { error ->
                            isLoading = false
                            errorMessage =
                                error // Mostra l'errore rosso (es. "Password attuale errata")
                        }
                    )
                },
                enabled = !isLoading && currentPassword.isNotBlank() && newPassword.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PowerGreen,
                    disabledContainerColor = PowerGreen.copy(alpha = 0.6f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "AGGIORNA PASSWORD",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- DIALOG DI SUCCESSO ---
        if (showSuccessDialog) {
            Dialog(onDismissRequest = { }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PowerGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Password Aggiornata!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "La tua password è stata modificata con successo. Usa la nuova password al prossimo accesso.",
                            textAlign = TextAlign.Center,
                            color = TextGray
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                showSuccessDialog = false
                                onPasswordUpdated() // Torna indietro o vai alla home
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PowerGreen)
                        ) {
                            Text("OK, TORNA INDIETRO")
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTE CUSTOM PER I CAMPI PASSWORD ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RicaPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    imeAction: ImeAction,
    isError: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),

        // --- MODIFICA QUI I COLORI ---
        colors = OutlinedTextFieldDefaults.colors(
            // Testo inserito
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,

            // Cursore che lampeggia
            cursorColor = Color.Black,

            // Bordo (Outline)
            focusedBorderColor = Color.Black,   // Quando scrivi
            unfocusedBorderColor = Color.Black, // Quando non scrivi

            // Etichetta (Label in alto)
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.Black, // O Color.Gray se preferisci contrasto

            // Icona Occhio
            focusedTrailingIconColor = Color.Black,
            unfocusedTrailingIconColor = Color.Black,

            // Colori errore (opzionale mantenerli rossi o farli neri)
            errorBorderColor = ErrorColor,
            errorLabelColor = ErrorColor,
            errorTextColor = Color.Black,
        ),
        // -----------------------------

        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction
        ),
        trailingIcon = {
            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
            val description = if (passwordVisible) "Nascondi password" else "Mostra password"

            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, contentDescription = description, tint = Color.Black) // Anche l'icona nera
            }
        }
    )
}