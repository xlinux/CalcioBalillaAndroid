package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biliardino.model.LeagueResponse
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLeaguesScreen(s: UiState, vm: AppViewModel) {
    var showTypeSelection by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var leagueToJoin by remember { mutableStateOf<LeagueResponse?>(null) }

    PullToRefreshBox(
        isRefreshing = s.loading,
        onRefresh = {
            vm.loadMyLeagues()
            vm.loadPublicLeagues()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            val myLeagueIds = s.myLeagues.map { it.id }.toSet()
            val otherLeagues = s.publicLeagues.filter { it.id !in myLeagueIds }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (s.myLeagues.isNotEmpty()) {
                    item {
                        LeagueSectionHeader(
                            title = "Le mie leghe",
                            count = s.myLeagues.size
                        )
                    }
                    items(s.myLeagues) { league ->
                        LeagueCard(
                            league = league,
                            onCLick = { vm.selectLeague(league) },
                            showInviteCode = true
                        )
                    }
                }

                if (otherLeagues.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        LeagueSectionHeader(
                            title = "Altre leghe",
                            count = otherLeagues.size
                        )
                    }
                    items(otherLeagues) { league ->
                        LeagueCard(
                            league = league,
                            onCLick = {
                                leagueToJoin = league
                                showJoinDialog = true
                            },
                            showInviteCode = false
                        )
                    }
                } else if (s.myLeagues.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        LeagueSectionHeader(
                            title = "Altre leghe",
                            count = 0
                        )
                        Spacer(Modifier.height(8.dp))
                        EmptyLeagueCard("Nessuna altra lega disponibile")
                    }
                }

                if (s.myLeagues.isEmpty() && s.publicLeagues.isEmpty() && !s.loading) {
                    item {
                        Box(
                            Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyLeagueCard("Non sei ancora iscritto a nessuna lega. Creane una o unisciti a una esistente!")
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd)
            ) {
                FloatingActionButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Add, "Opzioni Lega")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Crea Nuova") },
                        onClick = {
                            showMenu = false
                            showTypeSelection = true
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

            if (showTypeSelection) {
                AlertDialog(
                    onDismissRequest = { showTypeSelection = false },
                    title = { Text("Cosa vuoi creare?") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CreationOptionCard(
                                title = "Lega privata",
                                description = "Ideale per gruppi di amici, colleghi o competizioni private.",
                                icon = Icons.Default.Security,
                                onClick = {
                                    vm.onNewLeagueTypeChange("PRIVATE_LEAGUE")
                                    showTypeSelection = false
                                    showCreateDialog = true
                                }
                            )
                            CreationOptionCard(
                                title = "Circolo / Club",
                                description = "Ideale per circoli sportivi, club, associazioni e attività organizzate.",
                                icon = Icons.Default.Groups,
                                onClick = {
                                    vm.onNewLeagueTypeChange("CLUB")
                                    showTypeSelection = false
                                    showCreateDialog = true
                                }
                            )
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showTypeSelection = false }) { Text("Annulla") }
                    }
                )
            }

            if (showCreateDialog) {
                val isClub = s.newLeagueType == "CLUB"
                val typeName = if (isClub) "Circolo" else "Lega"

                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("Nuovo $typeName") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = s.newLeagueName,
                                onValueChange = vm::onNewLeagueNameChange,
                                label = { Text("Nome $typeName") },
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
}

@Composable
private fun CreationOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyLeagueCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Nessuna lega",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
