package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biliardino.model.LeagueResponse
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun MyLeaguesScreen(s: UiState, vm: AppViewModel) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var leagueToJoin by remember { mutableStateOf<LeagueResponse?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (s.myLeagues.isNotEmpty()) {
                item {
                    Text("Le mie Leghe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                items(s.myLeagues) { league ->
                    LeagueCard(
                        league = league,
                        onCLick = { vm.selectLeague(league) },
                        showInviteCode = true
                    )
                }
            }

            // Filtriamo le leghe pubbliche per non mostrare quelle dove sono già iscritto
            val myLeagueIds = s.myLeagues.map { it.id }.toSet()
            val otherLeagues = s.publicLeagues.filter { it.id !in myLeagueIds }

            if (otherLeagues.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Altre Leghe", style = MaterialTheme.typography.titleLarge)
                }
                
                items(otherLeagues) { league ->
                    LeagueCard(
                        league = league,
                        onCLick = { 
                            // Obbligatorio chiedere il codice per utenti registrati
                            leagueToJoin = league
                            showJoinDialog = true
                        },
                        showInviteCode = false
                    )
                }
            } else if (s.myLeagues.isNotEmpty()) {
                // Se ho delle mie leghe ma non ci sono "altre" leghe
                item {
                    Spacer(Modifier.height(16.dp))
                    Text("Altre Leghe", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            "Nessuna lega presente",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (s.myLeagues.isEmpty() && s.publicLeagues.isEmpty() && !s.loading) {
                item {
                    Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Nessuna Lega",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Non sei ancora iscritto a nessuna lega. Creane una o unisciti a una esistente!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier
            .padding(16.dp)
            .align(Alignment.BottomEnd)) {
            FloatingActionButton(
                onClick = { showMenu = true }
            ) {
                Icon(Icons.Default.Add, "Opzioni Lega")
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Crea Nuova Lega") },
                    onClick = {
                        showMenu = false
                        showCreateDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Unisciti con Codice") },
                    onClick = {
                        showMenu = false
                        showJoinDialog = true
                    }
                )
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Nuova Lega") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = s.newLeagueName,
                            onValueChange = vm::onNewLeagueNameChange,
                            label = { Text("Nome Lega") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = s.newLeagueDescription,
                            onValueChange = vm::onNewLeagueDescriptionChange,
                            label = { Text("Descrizione") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.createLeague()
                        showCreateDialog = false
                    }) {
                        Text("Crea")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Annulla")
                    }
                }
            )
        }

        if (showJoinDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showJoinDialog = false 
                    leagueToJoin = null
                    vm.onInviteCodeChange("")
                },
                title = { Text(leagueToJoin?.let { "Unisciti a ${it.name}" } ?: "Unisciti a una Lega") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Inserisci il codice di invito per unirti a questa lega.")
                        OutlinedTextField(
                            value = s.inviteCode,
                            onValueChange = vm::onInviteCodeChange,
                            label = { Text("Codice Invito") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.joinLeague()
                        showJoinDialog = false
                        leagueToJoin = null
                    }) {
                        Text("Unisciti")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showJoinDialog = false 
                        leagueToJoin = null
                        vm.onInviteCodeChange("")
                    }) {
                        Text("Annulla")
                    }
                }
            )
        }
    }
}
