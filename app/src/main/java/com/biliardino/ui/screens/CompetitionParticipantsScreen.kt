package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
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
import com.biliardino.model.TeamResponse
import com.biliardino.ui.Screen
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
    val isSingle = competition.matchType == "SINGLE"
    var showAddDialog by remember { mutableStateOf(false) }
    var participantToRemove by remember { mutableStateOf<LeagueMemberResponse?>(null) }
    var teamToRemove by remember { mutableStateOf<TeamResponse?>(null) }
    
    val participantIds = remember(s.competitionPlayers) { s.competitionPlayers.map { it.userId }.toSet() }
    val availableMembers = remember(s.leagueMembers, participantIds) {
        s.leagueMembers.filter { it.userId !in participantIds }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            val isEmpty = if (isSingle) s.competitionPlayers.isEmpty() else s.seasonTeams.isEmpty()
            
            if (isEmpty && !s.loading) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            if (isSingle) "Nessun partecipante iscritto a questa competizione." else "Nessuna squadra iscritta a questa competizione.",
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
                            if (isSingle) "Partecipanti Iscritti" else "Squadre Iscritte",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (isSingle) {
                        items(s.competitionPlayers, key = { it.userId }) { participant ->
                            ParticipantCard(
                                participant = participant,
                                onRemove = { participantToRemove = participant }
                            )
                        }
                    } else {
                        items(s.seasonTeams, key = { it.id }) { team ->
                            TeamListItemForManagement(
                                team = team,
                                onRemove = { teamToRemove = team }
                            )
                        }
                    }
                }
            }
        }

        if ((s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER") && competition.registrationOpen) {
            FloatingActionButton(
                onClick = {
                    if (isSingle) {
                        showAddDialog = true
                        vm.loadLeagueMembersForParticipantPicker(league.id)
                    } else {
                        vm.navigateTo(Screen.CreateTeam(league, season, competition))
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    if (isSingle) Icons.Default.PersonAdd else Icons.Default.Groups, 
                    contentDescription = if (isSingle) "Aggiungi Partecipante" else "Crea Squadra"
                )
            }
        }
    }

    if (showAddDialog && isSingle) {
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
    teamToRemove?.let { team ->
        AlertDialog(
            onDismissRequest = { teamToRemove = null },
            title = { Text("Rimuovere squadra?") },
            text = { Text("Vuoi rimuovere la squadra ${team.name} da ${competition.name}? Questa operazione eliminerà anche eventuali risultati collegati.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteCompetitionTeam(competition.id, team.id)
                        teamToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Rimuovi")
                }
            },
            dismissButton = {
                TextButton(onClick = { teamToRemove = null }) {
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

@Composable
private fun TeamListItemForManagement(team: TeamResponse, onRemove: () -> Unit) {
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
                Text(team.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (team.playerAUsername != null) {
                    Text(
                        "${team.playerAUsername}${if (team.playerBUsername != null) " & ${team.playerBUsername}" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Rimuovi", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
