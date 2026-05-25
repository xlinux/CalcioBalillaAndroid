package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biliardino.model.*
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun SeasonTeamsScreen(league: LeagueResponse, season: SeasonResponse, s: UiState, vm: AppViewModel) {
    CreateTeamView(league, s.seasonUsers, vm) { request ->
        vm.createTeam(season.id, request)
    }
}

@Composable
fun CreateTeamView(
    league: LeagueResponse,
    users: List<LeagueUserResponse>,
    vm: AppViewModel,
    onConfirm: (CreateTeamRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var playerA by remember { mutableStateOf<LeagueUserResponse?>(null) }
    var playerB by remember { mutableStateOf<LeagueUserResponse?>(null) }
    var showGuestDialog by remember { mutableStateOf(false) }
    var targetPlayerSlot by remember { mutableStateOf<Int?>(null) } // 1 for Player A, 2 for Player B

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crea una nuova squadra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nome Squadra") },
            modifier = Modifier.fillMaxWidth()
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("GIOCATORE 1", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { 
                    targetPlayerSlot = 1
                    showGuestDialog = true 
                }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Guest", fontSize = 12.sp)
                }
            }
            SearchablePlayerDropdown(
                users = users.filter { it.userId != playerB?.userId },
                selectedUser = playerA,
                onUserSelected = { playerA = it }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("GIOCATORE 2", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { 
                    targetPlayerSlot = 2
                    showGuestDialog = true 
                }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Guest", fontSize = 12.sp)
                }
            }
            SearchablePlayerDropdown(
                users = users.filter { it.userId != playerA?.userId },
                selectedUser = playerB,
                onUserSelected = { playerB = it }
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            enabled = name.isNotBlank() && playerA != null && playerB != null && playerA?.userId != playerB?.userId,
            onClick = {
                onConfirm(
                    CreateTeamRequest(
                        name = name,
                        playerAId = playerA!!.userId,
                        playerBId = playerB!!.userId
                    )
                )
                name = ""
                playerA = null
                playerB = null
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("CREA SQUADRA", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showGuestDialog) {
        CreateGuestPlayerDialog(
            onDismiss = { showGuestDialog = false },
            onConfirm = { username ->
                vm.createGuestPlayer(league.id, username) { newUser ->
                    if (targetPlayerSlot == 1) playerA = newUser
                    if (targetPlayerSlot == 2) playerB = newUser
                    showGuestDialog = false
                }
            }
        )
    }
}

@Composable
fun CreateGuestPlayerDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var username by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crea Giocatore Guest") },
        text = {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nome Giocatore") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = username.isNotBlank(),
                onClick = { onConfirm(username) }
            ) { Text("Crea") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

@Composable
fun SearchablePlayerDropdown(
    users: List<LeagueUserResponse>,
    selectedUser: LeagueUserResponse?,
    onUserSelected: (LeagueUserResponse) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredUsers = remember(searchQuery, users) {
        if (searchQuery.isBlank()) users
        else users.filter { it.username.contains(searchQuery, ignoreCase = true) }
    }

    Box {
        OutlinedButton(
            onClick = { expanded = true; searchQuery = "" },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedUser?.username ?: "Scegli giocatore...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 400.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Cerca giocatore...") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true
            )
            
            if (filteredUsers.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nessun risultato") },
                    onClick = { },
                    enabled = false
                )
            } else {
                filteredUsers.forEach { user ->
                    DropdownMenuItem(
                        text = { Text(user.username) },
                        onClick = {
                            onUserSelected(user)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
