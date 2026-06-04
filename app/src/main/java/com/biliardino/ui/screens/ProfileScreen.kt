package com.biliardino.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun ProfileScreen(s: UiState, vm: AppViewModel) {
    var newName by remember { mutableStateOf(s.currentUser?.name.orEmpty()) }
    var showDeleteAccountConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(s.currentUser?.name) {
        newName = s.currentUser?.name.orEmpty()
    }

    if (s.currentUser == null && s.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = s.currentUser
    val canSaveName = newName.isNotBlank() && newName.trim() != user?.name && !s.loading

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ProfileHeader(
                name = user?.name ?: "Utente",
                email = user?.email ?: "Email non disponibile",
                provider = "Google"
            )
        }

        item {
            ProfileSectionTitle("Account")
            ProfileSection {
                ProfileInfoRow(
                    icon = Icons.Default.Email,
                    title = "Email",
                    value = user?.email ?: "Non disponibile"
                )

                HorizontalDivider(Modifier.padding(start = 50.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nome visualizzato") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = { vm.updateProfile(newName.trim()) },
                        enabled = canSaveName,
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Salva nome")
                    }
                }
            }
        }

        item {
            ProfileSectionTitle("Sicurezza")
            ProfileSection {
                ProfileToggleRow(
                    icon = Icons.Default.Fingerprint,
                    title = "Accesso biometrico",
                    subtitle = if (s.canUseBiometrics) {
                        "Usa impronta o blocco dispositivo per accedere"
                    } else {
                        "Non disponibile su questo dispositivo"
                    },
                    checked = s.isBiometricEnabled,
                    enabled = s.canUseBiometrics,
                    onCheckedChange = vm::enableBiometric
                )

                HorizontalDivider(Modifier.padding(start = 50.dp))

                ProfileInfoRow(
                    icon = Icons.Default.VerifiedUser,
                    title = "Metodo di accesso",
                    value = "Google"
                )

                HorizontalDivider(Modifier.padding(start = 50.dp))
                Text(
                    text = "Accesso e sicurezza dell'account sono gestiti da Google.",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            ProfileSectionTitle("Aspetto")
            ProfileSection {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Tema dell'app",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionCard(
                            label = "Chiaro",
                            icon = Icons.Default.LightMode,
                            selected = s.theme == "LIGHT",
                            onClick = { vm.setTheme("LIGHT") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionCard(
                            label = "Scuro",
                            icon = Icons.Default.DarkMode,
                            selected = s.theme == "DARK",
                            onClick = { vm.setTheme("DARK") },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionCard(
                            label = "Sistema",
                            icon = Icons.Default.BrightnessAuto,
                            selected = s.theme == "SYSTEM",
                            onClick = { vm.setTheme("SYSTEM") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = vm::logout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Esci dall'account", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = { showDeleteAccountConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Elimina account", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showDeleteAccountConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirmation = false },
            title = { Text("Eliminare il tuo account?") },
            text = { Text("Sei sicuro di voler eliminare il tuo account? I tuoi dati personali verranno rimossi e verrai disconnesso dall'app. L'operazione non può essere annullata.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteAccount()
                        showDeleteAccountConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("SÌ, ELIMINA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirmation = false }) {
                    Text("ANNULLA")
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader(name: String, email: String, provider: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profileInitials(name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    provider,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(
        title.uppercase(),
        modifier = Modifier.padding(start = 4.dp, bottom = 7.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ProfileSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        content = { Column(content = content) }
    )
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileRowIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ProfileToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileRowIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProfileRowIcon(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.padding(9.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ThemeOptionCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor, contentColor = contentColor),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(21.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun profileInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "U"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}
