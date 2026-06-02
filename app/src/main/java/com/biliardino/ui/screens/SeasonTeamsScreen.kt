package com.biliardino.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biliardino.model.*
import com.biliardino.ui.Screen
import com.biliardino.ui.components.SearchablePlayerDropdown
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun SeasonTeamsScreen(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse?, s: UiState, vm: AppViewModel) {
    var showCreateForm by remember { mutableStateOf(false) }

    if (season.active == false) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Stagione Conclusa",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Non è più possibile gestire le squadre per questa stagione.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    } else if (competition == null) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Seleziona una competizione per gestire le squadre.", textAlign = TextAlign.Center)
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            if (showCreateForm) {
                CreateTeamView(
                    users = s.seasonUsers,
                    matchType = competition.matchType,
                    onConfirm = { request ->
                        vm.createTeam(competition.id, request)
                        showCreateForm = false
                    },
                    onCancel = { showCreateForm = false }
                )
            } else {
                Column(Modifier.fillMaxSize()) {
                    if (s.seasonTeams.isEmpty() && !s.loading) {
                        Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Nessuna squadra creata per questa competizione.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    "Squadre della Competizione",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            items(s.seasonTeams) { team ->
                                TeamListItem(team, competition.matchType) {
                                    vm.loadTeamDetailData(competition.id, team.id)
                                }
                            }
                        }
                    }
                }

                val isAdmin = s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER"
                val isJoined = competition.currentUserJoined
                val isTeamType = competition.matchType == "TEAM"
                val isDouble = competition.matchType == "DOUBLE"
                val isSingle = competition.matchType == "SINGLE"
                val hasMatches = s.seasonMatches.isNotEmpty()
                
                // Logic for showing Create Team button
                val showCreateButton = when {
                    // Competition must have registration open
                    competition.registrationOpen == false -> false
                    // Competition must be active
                    competition.status != "ACTIVE" && competition.status != null -> false
                    // If matches already exist in a scheduled competition, no more teams/entries allowed
                    competition.matchCreationMode == "SCHEDULED" && hasMatches -> false
                    // TEAM: Show only if user not already in a team
                    isTeamType -> {
                        val currentUserId = s.currentUser?.userId
                        val isUserInAnyTeam = s.seasonTeams.any { team -> 
                            team.playerAId == currentUserId || team.playerBId == currentUserId
                        }
                        !isUserInAnyTeam
                    }
                    // DOUBLE/SINGLE + SCHEDULED: allowed before calendar generation
                    (isDouble || isSingle) && competition.matchCreationMode == "SCHEDULED" -> true
                    // DOUBLE/SINGLE + FREE: entries are created during match registration, no need for "Create Team" tab button
                    else -> false
                }

                val fabLabel = when {
                    isTeamType -> "Crea Squadra"
                    isDouble -> "Aggiungi Coppia"
                    isSingle -> "Aggiungi Giocatore"
                    else -> "Crea Squadra"
                }

                if (showCreateButton && (isAdmin || isJoined)) {
                    FloatingActionButton(
                        onClick = { showCreateForm = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(fabLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeamListItem(team: TeamResponse, matchType: String?, onClick: () -> Unit) {
    val isTeamType = matchType == "TEAM"
    val isSingle = matchType == "SINGLE"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        ListItem(
            headlineContent = { Text(team.name, fontWeight = FontWeight.Bold) },
            supportingContent = {
                val subtitle = when {
                    isTeamType -> null
                    isSingle -> team.playerAUsername
                    else -> if (team.playerAUsername != null && team.playerBUsername != null) {
                        "${team.playerAUsername} & ${team.playerBUsername}"
                    } else {
                        team.playerAUsername ?: team.playerBUsername
                    }
                }
                subtitle?.let { Text(it) }
            },
            trailingContent = {
                if (!isTeamType) {
                    team.rating?.let {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = it.toString(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun CreateTeamView(
    users: List<LeagueUserResponse>,
    matchType: String?,
    onConfirm: (CreateTeamRequest) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var playerA by remember { mutableStateOf<LeagueUserResponse?>(null) }
    var playerB by remember { mutableStateOf<LeagueUserResponse?>(null) }

    val isTeamType = matchType == "TEAM"
    val isSingle = matchType == "SINGLE"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Nuova Squadra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        if (users.isEmpty() && !isTeamType) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    "Nessun giocatore iscritto a questa competizione. Aggiungi prima i partecipanti.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome squadra") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            if (!isTeamType) {
                TeamFormSectionCard(title = "Giocatori") {
                    PlayerSelectionRow(
                        label = if (isSingle) "Giocatore" else "Giocatore A",
                        users = users.filter { it.userId != playerB?.userId },
                        selectedUser = playerA,
                        onUserSelected = { playerA = it }
                    )
                    if (!isSingle) {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        PlayerSelectionRow(
                            label = "Giocatore B",
                            users = users.filter { it.userId != playerA?.userId },
                            selectedUser = playerB,
                            onUserSelected = { playerB = it }
                        )
                    }
                }
            }

            val canCreate = when {
                isTeamType -> name.isNotBlank()
                isSingle -> name.isNotBlank() && playerA != null
                else -> name.isNotBlank() && playerA != null && playerB != null && playerA?.userId != playerB?.userId
            }

            Button(
                enabled = canCreate,
                onClick = {
                    onConfirm(
                        CreateTeamRequest(
                            name = name,
                            playerAId = playerA?.userId ?: 0,
                            playerBId = playerB?.userId ?: 0
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Crea Squadra", style = MaterialTheme.typography.titleMedium)
            }
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Annulla")
        }
    }
}

@Composable
private fun TeamFormSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun PlayerSelectionRow(
    label: String,
    users: List<LeagueUserResponse>,
    selectedUser: LeagueUserResponse?,
    onUserSelected: (LeagueUserResponse) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(10.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(
                selectedUser?.username ?: "Seleziona",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        SearchablePlayerDropdown(
            users = users,
            selectedUser = selectedUser,
            onUserSelected = onUserSelected
        )
    }
}
