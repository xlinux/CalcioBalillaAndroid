package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun AuthScreen(s: UiState, vm: AppViewModel) {
    var isRegister by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        if (isRegister) {
            OutlinedTextField(s.username, vm::onUsernameChange, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(s.email, vm::onEmailChange, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(s.password, vm::onPasswordChange, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        
        Button(onClick = { if (isRegister) vm.register() else vm.login() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isRegister) "Registrati" else "Accedi")
        }
        
        TextButton(onClick = { isRegister = !isRegister }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (isRegister) "Hai già un account? Accedi" else "Non hai un account? Registrati")
        }
    }
}
