package NEW_VERSION.profile
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// --- COLORI COSTANTI ---
private val PowerGreen = Color(0xFF2E7D32)
private val TextBlack = Color(0xFF1A1A1A)
private val TextGray = Color(0xFF757575)
private val CardBg = Color(0xFFF5F5F5)

// --- MODELLO DATI SEMPLICE ---
data class PaymentMethod(
    val id: String,
    val last4: String,
    val holderName: String,
    val expiry: String, // MM/YY
    val isDefault: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    // --- 2. RECUPERA I DATI REALI DELL'UTENTE ---
    val user by authViewModel.userProfile.collectAsState()
    // Se l'utente non ha impostato un nome, usa "Utente Rica Rica" o la mail
    val realUserName = user?.userName ?: user?.email?.substringBefore("@") ?: "Utente Rica Rica"

    // --- 3. USA IL NOME REALE NELLA LISTA ---
    // Inizializziamo la lista usando 'realUserName' come intestatario
    var paymentMethods by remember { mutableStateOf(listOf(
        PaymentMethod("1", "4242", realUserName.uppercase(), "12/26", true),
        PaymentMethod("2", "8899", realUserName.uppercase(), "09/25", false)
    )) }

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        // ... (Il resto dello Scaffold rimane identico a prima) ...
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Metodi di Pagamento", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextBlack)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White),
                modifier = Modifier.shadow(2.dp)
            )
        },
        containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PowerGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi Carta")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // ... (Il codice UI della carta grafica rimane uguale) ...

            // HEADER: CARTA GRAFICA
            val defaultCard = paymentMethods.find { it.isDefault }
            if (defaultCard != null) {
                Text(
                    "Metodo Predefinito",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextGray
                )
                CreditCardVisual(defaultCard)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Le tue carte",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // LISTA CARTE
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(paymentMethods) { method ->
                    PaymentMethodItem(
                        method = method,
                        onSelect = { selectedId ->
                            paymentMethods = paymentMethods.map {
                                it.copy(isDefault = it.id == selectedId)
                            }
                        },
                        onDelete = { deleteId ->
                            paymentMethods = paymentMethods.filter { it.id != deleteId }
                        }
                    )
                }
            }
        }

        // --- DIALOG CON IL NOME AUTOMATICO ---
        if (showAddDialog) {
            AddCardDialog(
                initialName = realUserName.uppercase(), // <--- Passiamo il nome automatico al Dialog
                onDismiss = { showAddDialog = false },
                onAdd = { number, holder, expiry ->
                    val newCard = PaymentMethod(
                        id = System.currentTimeMillis().toString(),
                        last4 = number.takeLast(4),
                        holderName = holder,
                        expiry = expiry,
                        isDefault = false
                    )
                    paymentMethods = paymentMethods + newCard
                    showAddDialog = false
                }
            )
        }
    }
}


// --- MODIFICA AL DIALOG PER ACCETTARE IL NOME ---
@Composable
fun AddCardDialog(
    initialName: String, // <--- Parametro nuovo
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var number by remember { mutableStateOf("") }
    var holder by remember { mutableStateOf(initialName) } // <--- Pre-compila il nome!
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Aggiungi Carta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))

                BlackOutlinedTextField(value = number, onValueChange = { if (it.length <= 16) number = it }, label = "Numero Carta", type = KeyboardType.Number)
                Spacer(Modifier.height(12.dp))

                // Qui l'utente troverà già il suo nome scritto
                BlackOutlinedTextField(value = holder, onValueChange = { holder = it }, label = "Intestatario", type = KeyboardType.Text)

                Spacer(Modifier.height(12.dp))

                // ... (Resto del Dialog uguale a prima) ...
                Row(Modifier.fillMaxWidth()) {
                    Box(Modifier.weight(1f)) {
                        BlackOutlinedTextField(value = expiry, onValueChange = { if (it.length <= 5) expiry = it }, label = "MM/YY", type = KeyboardType.Number)
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        BlackOutlinedTextField(value = cvv, onValueChange = { if (it.length <= 3) cvv = it }, label = "CVV", type = KeyboardType.Number)
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if(number.isNotEmpty() && holder.isNotEmpty()) onAdd(number, holder, expiry)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PowerGreen)
                ) {
                    Text("SALVA CARTA", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
// --- COMPONENTI UI ---

@Composable
fun CreditCardVisual(method: PaymentMethod) {
    // Gradiente verde scuro per la carta grafica
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF1B5E20), Color(0xFF4CAF50))
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Chip e Icona Contactless
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp, 30.dp)
                            .background(Color(0xFFFFD700), RoundedCornerShape(4.dp))
                    )
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Numero Carta (Asteriscato)
                Text(
                    text = "**** **** **** ${method.last4}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Medium
                )

                // Footer (Nome e Scadenza)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TITOLARE", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Text(method.holderName.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SCADENZA", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Text(method.expiry, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodItem(
    method: PaymentMethod,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val borderColor = if (method.isDefault) PowerGreen else Color.Transparent
    val bgColor = if (method.isDefault) PowerGreen.copy(alpha = 0.05f) else CardBg

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onSelect(method.id) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icona Carta
        Icon(
            imageVector = Icons.Default.CreditCard,
            contentDescription = null,
            tint = TextBlack,
            modifier = Modifier.size(32.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "**** ${method.last4}",
                fontWeight = FontWeight.Bold,
                color = TextBlack,
                fontSize = 16.sp
            )
            Text(
                text = "Scade il ${method.expiry}",
                color = TextGray,
                fontSize = 12.sp
            )
        }

        // Selezione / Default
        if (method.isDefault) {
            Icon(Icons.Default.RadioButtonChecked, null, tint = PowerGreen)
        } else {
            // Tasto elimina solo se non è default
            IconButton(onClick = { onDelete(method.id) }) {
                Icon(Icons.Default.Delete, null, tint = Color.Gray)
            }
        }
    }
}


// Utility per TextField Nero (come richiesto prima)
@Composable
fun BlackOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    type: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = type),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            cursorColor = Color.Black,
            focusedBorderColor = Color.Black,
            unfocusedBorderColor = Color.Black,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.Black
        )
    )
}