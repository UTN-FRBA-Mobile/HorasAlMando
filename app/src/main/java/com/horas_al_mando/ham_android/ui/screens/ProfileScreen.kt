package com.horas_al_mando.ham_android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.horas_al_mando.ham_android.R
import com.horas_al_mando.ham_android.model.UserProfile
import com.horas_al_mando.ham_android.network.AuthRepository
import com.horas_al_mando.ham_android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(repository: AuthRepository, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val errorLoadProfile = stringResource(R.string.profile_error_load)
    val errorNoUserId = stringResource(R.string.profile_error_no_user_id)

    LaunchedEffect(Unit) {
        scope.launch {
            val userId = repository.getUserId()
            if (userId != null) {
                val response = repository.getUserProfile(userId)
                if (response.isSuccessful) {
                    userProfile = response.body()
                } else {
                    errorMessage = errorLoadProfile.format(response.code())
                }
            } else {
                errorMessage = errorNoUserId
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium)

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
        } else if (errorMessage != null) {
            Text(text = errorMessage!!, color = Destructive)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = BorderStroke(1.dp, Outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    userProfile?.let {
                        ProfileRow(label = stringResource(R.string.profile_label_name), value = "${it.firstName} ${it.lastName}")
                        HorizontalDivider(color = Outline, modifier = Modifier.padding(vertical = 8.dp))
                        ProfileRow(label = stringResource(R.string.profile_label_email), value = it.email)
                        HorizontalDivider(color = Outline, modifier = Modifier.padding(vertical = 8.dp))
                        ProfileRow(label = stringResource(R.string.profile_label_username), value = "@${it.username}")
                        HorizontalDivider(color = Outline, modifier = Modifier.padding(vertical = 8.dp))
                        ProfileRow(label = stringResource(R.string.profile_label_license), value = it.license ?: stringResource(R.string.profile_license_none))
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                scope.launch {
                    repository.logout()
                    onLogout()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, Outline),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(stringResource(R.string.profile_logout_button), fontWeight = FontWeight.SemiBold, color = Destructive)
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Secondary)
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
