package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biliardino.model.*
import com.biliardino.ui.components.MatchList
import com.biliardino.ui.components.ScorePicker
import com.biliardino.ui.components.SearchableTeamDropdown
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun CompetitionMatchesScreen(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse, s: UiState, vm: AppViewModel) {
    var showMatchForm by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        if (showMatchForm) {
            MatchEntryView(
                teams = s.seasonTeams,
                onConfirm = { request ->
                    vm.createMatch(competition.id, request)
                    showMatchForm = false
                },
                onCancel = { showMatchForm = false }
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                if (s.seasonMatches.isEmpty() && !s.loading) {
                    Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Nessuna partita registrata per questa competizione.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(Modifier.weight(1f)) {
                        val isAdmin = s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER"
                        MatchList(
                            matches = s.seasonMatches,
                            teams = s.seasonTeams,
                            isAdmin = isAdmin,
                            rankingType = competition.competitionRankingType,
                            calendarGenerationMode = competition.calendarGenerationMode,
                            competitionType = competition.type,
                            onDeleteMatch = { matchId -> vm.deleteMatch(competition.id, matchId) },
                            onUpdateResult = { matchId, sA, sB -> vm.updateMatchResult(competition.id, matchId, sA, sB) }
                        )
                    }
                }
            }

            val isAdmin = s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER"
            val isJoined = competition.currentUserJoined
            val showAddButton = competition.matchCreationMode == "FREE" && 
                                (competition.status == "ACTIVE" || competition.status == null) && 
                                (isAdmin || isJoined)

            if (showAddButton) {
                FloatingActionButton(
                    onClick = { showMatchForm = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuova Partita")
                }
            }
        }
    }
}

@Composable
fun MatchEntryView(teams: List<TeamResponse>, onConfirm: (CreateDoubleMatchRequest) -> Unit, onCancel: () -> Unit) {
    var teamA by remember { mutableStateOf<TeamResponse?>(null) }
    var teamB by remember { mutableStateOf<TeamResponse?>(null) }
    var scoreA by remember { mutableIntStateOf(0) }
    var scoreB by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Nuova Partita",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (teams.size < 2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "Servono almeno 2 squadre per registrare una partita.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Squadre", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    
                    TeamSelectionRow(
                        label = "Squadra A",
                        teams = teams.filter { it.id != teamB?.id && !haveSharedPlayers(it, teamB) },
                        selectedTeam = teamA,
                        onTeamSelected = { teamA = it }
                    )
                    
                    HorizontalDivider(thickness = 0.5.dp)
                    
                    TeamSelectionRow(
                        label = "Squadra B",
                        teams = teams.filter { it.id != teamA?.id && !haveSharedPlayers(it, teamA) },
                        selectedTeam = teamB,
                        onTeamSelected = { teamB = it }
                    )
                }
            }

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Risultato", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScorePicker("A", scoreA, { scoreA = it })
                        Text("-", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ScorePicker("B", scoreB, { scoreB = it })
                    }
                }
            }

            Button(
                enabled = teamA != null && teamB != null && teamA?.id != teamB?.id && !haveSharedPlayers(teamA, teamB),
                onClick = {
                    onConfirm(
                        CreateDoubleMatchRequest(
                            teamAId = teamA!!.id,
                            teamBId = teamB!!.id,
                            scoreA = scoreA,
                            scoreB = scoreB
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Salva partita", style = MaterialTheme.typography.titleMedium)
            }
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Annulla")
        }
    }
}

private fun haveSharedPlayers(teamA: TeamResponse?, teamB: TeamResponse?): Boolean {
    if (teamA == null || teamB == null) return false
    val playersA = setOf(teamA.playerAId, teamA.playerBId)
    val playersB = setOf(teamB.playerAId, teamB.playerBId)
    return playersA.intersect(playersB).isNotEmpty()
}

@Composable
private fun TeamSelectionRow(
    label: String,
    teams: List<TeamResponse>,
    selectedTeam: TeamResponse?,
    onTeamSelected: (TeamResponse) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            selectedTeam?.let {
                Text(
                    "${it.playerAUsername} + ${it.playerBUsername}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        SearchableTeamDropdown(
            teams = teams,
            selectedTeam = selectedTeam,
            onTeamSelected = onTeamSelected
        )
    }
}
