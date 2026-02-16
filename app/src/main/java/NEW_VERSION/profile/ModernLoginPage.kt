package com.example.ricarica.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ricarica.profile.AuthViewModel

private val GreenPrimary = Color(0xFF2E7D32)
private val BackgroundColor = Color(0xFFF5F5F5)
private val TextBlack = Color(0xFF1A1A1A)

@Composable
fun ModernLoginPage(
    authViewModel: AuthViewModel,
    onSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    onGuestClick: () -> Unit // <--- NUOVO PARAMETRO
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = BackgroundColor) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ... (TUTTO IL CODICE HEADER E INPUT RIMANE UGUALE A PRIMA) ...
            // Box Icona, Testi, Input Email, Input Password...
            // Copia pure la parte superiore dal file precedente fino al Button "ACCEDI"

            // --- HEADER / LOGO ---
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Bentornato!", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextBlack)
            Text("Accedi per continuare a noleggiare", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

            // INPUT EMAIL
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    focusedLabelColor = GreenPrimary,
                    cursorColor = GreenPrimary,
                    focusedTextColor = TextBlack,
                    unfocusedTextColor = TextBlack
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // INPUT PASSWORD
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") }, leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null) } },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    focusedLabelColor = GreenPrimary,
                    focusedTextColor = TextBlack,
                    unfocusedTextColor = TextBlack,
                    cursorColor = GreenPrimary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))

            // BOTTONE ACCEDI
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    authViewModel.login(
                        email = email, pass = password,
                        onSuccess = { isLoading = false; onSuccess() },
                        onError = { msg -> isLoading = false; errorMessage = msg }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("ACCEDI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // LINK REGISTRAZIONE
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Non hai un account?", color = Color.Gray)
                TextButton(onClick = onRegisterClick) {
                    Text("Registrati", fontWeight = FontWeight.Bold, color = GreenPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 40.dp))
            Spacer(modifier = Modifier.height(16.dp))

            // --- NUOVO: BOTTONE OSPITE ---
            Text(
                text = "Continua come ospite",
                color = TextBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onGuestClick() }
                    .padding(8.dp) // Aumenta area click
            )
        }
    }
}