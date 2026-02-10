package com.example.ricarica.profile
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController


// --- COLORI E STILI ---
private val BackgroundColor = Color(0xFFF8F9FA)
private val CardBackgroundWhite = Color(0xFFFFFFFF)
private val TextBlack = Color(0xFF1A1A1A)
private val TextGray = Color(0xFF757575)
private val DividerColor = Color(0xFFEEEEEE)
private val IconColorGray = Color(0xFF616161)
private val LogoutRed = Color(0xFFD32F2F)
private val ProfilePlaceholderColor = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernProfilePage(
    navController: NavController,
    authViewModel: AuthViewModel,
    onLogoutClick: () -> Unit
) {
    // 1. CORREZIONE: Sintassi corretta per osservare il Flow. Niente "remember {}".
    val userProfile by authViewModel.userProfile.collectAsState()

    // 2. CORREZIONE: Gestione sicura dei dati (se userProfile è null, usiamo valori default)
    val displayName = userProfile?.userName ?: "Utente"
    val displayDate = userProfile?.email ?: "Membro"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "PROFILO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextBlack
                    )
                },

                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundColor,
                    scrolledContainerColor = BackgroundColor
                )
            )
        },
        containerColor = BackgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // --- HEADER PROFILO ---
            item {
                // 3. CORREZIONE: Passo le variabili calcolate sopra
                ProfileHeader(userName = displayName, joinDate = displayDate)
            }

            // --- SEZIONE ACCOUNT SETTINGS ---
            item {
                ProfileCardSection(title = "Impostazioni account") {
                    ProfileMenuItem(
                        icon = Icons.Outlined.Person,
                        title = "Modifica profilo",
                        onClick = { navController.navigate("edit_profile") }
                    )
                    ProfileDivider()
                    ProfileMenuItem(
                        icon = Icons.Outlined.Lock,
                        title = "Modifica Password",
                        onClick = { /* Naviga a Change Password */ }
                    )
                }
            }

            // --- SEZIONE MY ACTIVITY ---
            item {
                ProfileCardSection(title = "La mia attività") {
                    ProfileMenuItem(
                        icon = Icons.Outlined.History,
                        title = "Storico noleggi",
                        onClick = { navController.navigate("rental_history")}
                    )
                    ProfileDivider()
                    ProfileMenuItem(
                        icon = Icons.Outlined.CreditCard,
                        title = "Metodo di pagamento",
                        onClick = { /* Naviga ai pagamenti */ }
                    )
                }
            }


            // --- BOTTONE LOGOUT ---
            item {
                Spacer(modifier = Modifier.height(16.dp))
                LogoutButton(onClick = onLogoutClick)
            }
        }
    }
}

// --- COMPONENTI RIUTILIZZABILI ---

@Composable
fun ProfileHeader(userName: String, joinDate: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(ProfilePlaceholderColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = "Change Photo",
                tint = IconColorGray,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = userName,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = joinDate,
            fontSize = 14.sp,
            color = TextGray
        )
    }
}

@Composable
fun ProfileCardSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackgroundWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IconColorGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextBlack
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = IconColorGray.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = DividerColor,
        thickness = 1.dp
    )
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LogoutRed,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ExitToApp,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Log Out",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

