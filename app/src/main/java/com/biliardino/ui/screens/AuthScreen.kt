package com.biliardino.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun AuthScreen(s: UiState, vm: AppViewModel) {
    var isRegister by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val fragmentActivity = remember(context) { context.findFragmentActivity() }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        if (isRegister) {
            OutlinedTextField(s.username, vm::onUsernameChange, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(s.email, vm::onEmailChange, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = s.password,
            onValueChange = vm::onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            trailingIcon = if (!isRegister && s.canUseBiometrics && s.isBiometricEnabled && fragmentActivity != null) {
                {
                    IconButton(onClick = { vm.showBiometricPrompt(fragmentActivity) }) {
                        Icon(Icons.Default.Fingerprint, contentDescription = "Biometric Login", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            } else null
        )
        Spacer(Modifier.height(16.dp))
        
        Button(onClick = { if (isRegister) vm.register() else vm.login() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isRegister) "Registrati" else "Accedi")
        }

        if (s.canUseBiometrics && fragmentActivity != null && s.isBiometricEnabled) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { vm.showBiometricPrompt(fragmentActivity) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isRegister) "Usa Biometria per accedere" else "Accedi con Biometria / Touch ID")
            }
        }
        
        TextButton(onClick = { isRegister = !isRegister }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (isRegister) "Hai già un account? Accedi" else "Non hai un account? Registrati")
        }
    }
}
