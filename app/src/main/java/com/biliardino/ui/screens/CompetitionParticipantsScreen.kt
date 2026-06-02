package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biliardino.model.CompetitionResponse
import com.biliardino.model.LeagueMemberResponse
import com.biliardino.model.LeagueResponse
import com.biliardino.model.SeasonResponse
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun CompetitionParticipantsScreen(
    league: LeagueResponse,
    season: SeasonResponse,
    competition: CompetitionResponse,
    s: UiState,
    vm: AppViewModel
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var participantToRemove by remember { mutableStateOf<LeagueMemberResponse?>(null) }
    
    val participantIds = remember(s.competitionPlayers) { s.competitionPlayers.map { it.userId }.toSet() }
    val availableMembers = remember(s.leagueMembers, participantIds) {
        s.leagueMembers.filter { it.userId !in participantIds }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (s.competitionPlayers.isEmpty() && !s.loading) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            "Nessun partecipante iscritto a questa competizione.",
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "Partecipanti Iscritti",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(s.competitionPlayers, key = { it.userId }) { participant ->
                        ParticipantCard(
                            participant = participant,
                            onRemove = { participantToRemove = participant }
                        )
                    }
                }
            }
        }

        if ((s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER") && competition.registrationOpen) {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                    vm.loadLeagueMembersForParticipantPicker(league.id)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Aggiungi Partecipante")
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Aggiungi partecipante") },
            text = {
                if (availableMembers.isEmpty() && !s.loading) {
                    Text("Tutti i membri della lega sono già iscritti a questa competizione.")
                } else if (s.loading) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableMembers, key = { it.userId }) { member ->
                            ListItem(
                                headlineContent = { Text(member.username, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(member.email ?: member.role) },
                                trailingContent = {
                                    TextButton(
                                        onClick = {
                                            vm.addCompetitionPlayer(competition.id, member.userId)
                                            showAddDialog = false
                                        }
                                    ) {
                                        Text("Aggiungi")
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Chiudi")
                }
            }
        )
    }

    participantToRemove?.let { participant ->
        AlertDialog(
            onDismissRequest = { participantToRemove = null },
            title = { Text("Rimuovere partecipante?") },
            text = { Text("Vuoi rimuovere ${participant.username} da ${competition.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.removeCompetitionPlayer(competition.id, participant.userId)
                        participantToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Rimuovi")
                }
            },
            dismissButton = {
                TextButton(onClick = { participantToRemove = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun ParticipantCard(participant: LeagueMemberResponse, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(participant.username, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    participant.email ?: participant.role,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Rimuovi", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
