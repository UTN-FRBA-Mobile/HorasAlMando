package com.horas_al_mando.ham_android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.horas_al_mando.ham_android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var username     by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun mockLogin() {
        if (username.isBlank() || password.isBlank()) {
            errorMessage = "Completá todos los campos."
            return
        }
        scope.launch {
            isLoading = true
            delay(1000)
            isLoading = false
            onLoginSuccess()
        }
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientDark, GradientBlue))),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Surface),
            border    = BorderStroke(1.dp, Outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier            = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text       = "Horas Al Mando",
                    style      = MaterialTheme.typography.headlineLarge,
                    color      = Primary,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value         = username,
                    onValueChange = { username = it; errorMessage = null },
                    label         = { Text("Usuario") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )

                OutlinedTextField(
                    value                = password,
                    onValueChange        = { password = it; errorMessage = null },
                    label                = { Text("Contraseña") },
                    singleLine           = true,
                    modifier             = Modifier.fillMaxWidth(),
                    shape                = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                )

                errorMessage?.let {
                    Text(text = it, color = Destructive, style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick        = { mockLogin() },
                    modifier       = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    enabled        = !isLoading,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = OnPrimary,
                        )
                    } else {
                        Text("Ingresar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
