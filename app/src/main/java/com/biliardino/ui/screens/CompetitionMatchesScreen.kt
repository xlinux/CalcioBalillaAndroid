package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.sp
import com.biliardino.model.*
import com.biliardino.ui.Screen
import com.biliardino.ui.components.MatchList
import com.biliardino.ui.components.SearchableTeamDropdown
import com.biliardino.ui.components.groupMatchesByHeader
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun CompetitionMatchesScreen(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse, s: UiState, vm: AppViewModel) {
    val isFinalStage = competition.phase == "FINAL_STAGE" || competition.phase == "READY_FOR_FINAL_STAGE"
    
    // For tournaments in final stage, we render the entire CupCompetitionView
    // as requested, bypassing the standard unplayed matches list.
    if (competition.type == "TOURNAMENT" && isFinalStage) {
        CupCompetitionView(
            league = league,
            season = season,
            competition = competition,
            s = s.copy(currentCompetitionMode = "MAIN", currentCompetitionTab = 0),
            vm = vm
        )
        return
    }

    var showMatchForm by remember { mutableStateOf(false) }
    val isTeamType = competition.matchType == "TEAM" || competition.rankingMode == "TEAM"

    Box(Modifier.fillMaxSize()) {
        if (showMatchForm) {
            MatchEntryView(
                teams = s.seasonTeams,
                isTeamType = isTeamType,
                onConfirm = { request ->
                    vm.createMatch(competition.id, request)
                    showMatchForm = false
                },
                onCancel = { showMatchForm = false }
            )
        } else {
            val isAdmin = s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER"
            val isCompetitionActive = competition.active ?: (competition.status == "ACTIVE" || competition.status == null)
            
            if (s.seasonMatches.isEmpty() && !s.loading) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Nessuna partita registrata per questa competizione.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Group stage or Cup before bracket generation: show unplayed matches list
                val allUnplayed = s.seasonMatches.filter { it.scoreA == null || it.scoreB == null }
                
                if (allUnplayed.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nessuna partita da giocare.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    MatchList(
                        matches = allUnplayed,
                        teams = s.seasonTeams,
                        isAdmin = isAdmin,
                        rankingType = competition.competitionRankingType,
                        calendarGenerationMode = competition.calendarGenerationMode,
                        competitionType = competition.type,
                        isCompetitionActive = isCompetitionActive,
                        onDeleteMatch = { matchId: Long -> vm.deleteMatch(competition.id, matchId) },
                        onUpdateResult = { matchId: Long, sA: Int, sB: Int -> 
                            vm.updateMatchResult(competition.id, matchId, sA, sB).invokeOnCompletion {
                                vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
                            }
                        },
                        onEditResult = { matchId: Long, sA: Int, sB: Int -> 
                            vm.editMatchResult(competition.id, matchId, sA, sB).invokeOnCompletion {
                                vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
                            }
                        },
                        onMatchClick = { match: MatchResponse ->
                            vm.loadMatchComments(match.id)
                            vm.navigateTo(Screen.MatchDetail(match))
                        }
                    )
                }
            }

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
fun MatchEntryView(teams: List<TeamResponse>, isTeamType: Boolean = false, onConfirm: (CreateDoubleMatchRequest) -> Unit, onCancel: () -> Unit) {
    var teamA by remember { mutableStateOf<TeamResponse?>(null) }
    var teamB by remember { mutableStateOf<TeamResponse?>(null) }
    var scoreA by remember { mutableIntStateOf(0) }
    var scoreB by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                "Nuova Partita",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Registra un nuovo incontro e aggiorna le classifiche",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (teams.size < 2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    "Servono almeno 2 squadre per registrare una partita.",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "SELEZIONA SQUADRE", 
                        style = MaterialTheme.typography.labelLarge, 
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                    
                    TeamSelectionRow(
                        label = "Squadra A",
                        teams = teams.filter { it.id != teamB?.id && (isTeamType || !haveSharedPlayers(it, teamB)) },
                        selectedTeam = teamA,
                        isTeamType = isTeamType,
                        onTeamSelected = { teamA = it }
                    )
                    
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    TeamSelectionRow(
                        label = "Squadra B",
                        teams = teams.filter { it.id != teamA?.id && (isTeamType || !haveSharedPlayers(it, teamA)) },
                        selectedTeam = teamB,
                        isTeamType = isTeamType,
                        onTeamSelected = { teamB = it }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "RISULTATO FINALE", 
                        style = MaterialTheme.typography.labelLarge, 
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScoreEntryColumn(teamA?.name ?: "A", scoreA) { scoreA = it }
                        Text("-", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        ScoreEntryColumn(teamB?.name ?: "B", scoreB) { scoreB = it }
                    }
                }
            }

            Button(
                enabled = teamA != null && teamB != null && teamA?.id != teamB?.id && (isTeamType || !haveSharedPlayers(teamA, teamB)),
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
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("SALVA RISULTATO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("ANNULLA", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ScoreEntryColumn(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        
        FilledTonalIconButton(onClick = { onValueChange(value + 1) }) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
        
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.width(60.dp)
        ) {
            Text(
                text = value.toString(),
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
        
        FilledTonalIconButton(onClick = { if (value > 0) onValueChange(value - 1) }) {
            Text("-", style = MaterialTheme.typography.titleLarge)
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
    isTeamType: Boolean = false,
    onTeamSelected: (TeamResponse) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            if (!isTeamType) {
                selectedTeam?.let {
                    Text(
                        "${it.playerAUsername ?: ""} + ${it.playerBUsername ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        SearchableTeamDropdown(
            teams = teams,
            selectedTeam = selectedTeam,
            isTeamType = isTeamType,
            onTeamSelected = onTeamSelected
        )
    }
}
