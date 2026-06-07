package com.biliardino.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biliardino.model.*
import com.biliardino.ui.Screen
import androidx.compose.foundation.rememberScrollState
import com.biliardino.ui.components.MatchList
import com.biliardino.ui.components.MatchesContent
import com.biliardino.ui.components.TournamentBracketView
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentCompetitionView(
    league: LeagueResponse,
    season: SeasonResponse,
    competition: CompetitionResponse,
    s: UiState,
    vm: AppViewModel
) {
    val isGroups = competition.tournamentFormat == "GROUPS_THEN_SINGLE_ELIMINATION"
    
    val participantsTabLabel = when (competition.matchType) {
        "TEAM" -> "Squadre"
        "SINGLE" -> "Giocatori"
        else -> "Partecipanti"
    }

    val competitionMatches = s.seasonMatches.filter { it.competitionId == competition.id || it.competitionId == null } // The null check might be needed if competitionId isn't always present in match response
    val hasMatches = competitionMatches.isNotEmpty()
    val allGroupMatchesFinished = hasMatches && 
            competitionMatches.filter { !it.knockoutStage }.all { it.scoreA != null && it.scoreB != null }

    val tabs = if (isGroups) {
        mutableListOf("Gironi").apply {
            if (allGroupMatchesFinished) add("Fase Finale")
            add("Chat")
            add(participantsTabLabel)
        }
    } else {
        listOf("Tabellone", "Chat", participantsTabLabel)
    }

    val selectedTab = if (s.currentCompetitionTab >= tabs.size) 0 else s.currentCompetitionTab

    LaunchedEffect(Unit) {
        vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
    }

    LaunchedEffect(selectedTab) {
        val chatTabIndex = tabs.indexOf("Chat")
        val participantsTabIndex = tabs.indexOf(participantsTabLabel)
        
        if (selectedTab == chatTabIndex && chatTabIndex != -1) {
            vm.loadCompetitionComments(competition.id)
        }
        if (isGroups && selectedTab == 0) {
            vm.loadCompetitionGroups(competition.id)
        }
        if (selectedTab == participantsTabIndex && participantsTabIndex != -1) {
            if (competition.matchType == "SINGLE") {
                vm.loadCompetitionPlayers(competition.id)
            } else {
                vm.loadSeasonStatsData(league.id, season.id, competition.id)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (competition.active == false && (competition.winnerTeamId != null || competition.winnerUserId != null)) {
            val winnerName = if (competition.winnerTeamId != null) {
                s.seasonTeams.find { it.id == competition.winnerTeamId }?.name
            } else {
                s.seasonUsers.find { it.userId == competition.winnerUserId }?.username
            }

            if (winnerName != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆 Vincitore", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                        Text(winnerName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (s.currentCompetitionMode == "MAIN") {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { vm.updateCompetitionTab(index) },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Bold) }
                    )
                }
            }
        }

        Box(Modifier.weight(1f)) {
            when (s.currentCompetitionMode) {
                "MATCHES" -> CompetitionMatchesScreen(league, season, competition, s, vm)
                "HISTORY" -> TournamentMatchesTab(league, s, competition, vm, onlyPlayed = true)
                else -> {
                    val tabTitle = tabs[selectedTab]
                    when (tabTitle) {
                        "Gironi" -> TournamentGroupsTab(s, competition, vm)
                        "Tabellone" -> TournamentBracketView(
                            matches = s.seasonMatches,
                            onMatchClick = { match ->
                                vm.loadMatchComments(match.id)
                                vm.navigateTo(Screen.MatchDetail(match))
                            }
                        )
                        "Fase Finale" -> TournamentBracketView(
                            matches = s.seasonMatches.filter { it.knockoutStage },
                            onMatchClick = { match ->
                                vm.loadMatchComments(match.id)
                                vm.navigateTo(Screen.MatchDetail(match))
                            }
                        )
                        "Chat" -> CompetitionChatTab(competition.id, s, vm)
                        participantsTabLabel -> {
                            if (competition.matchType == "TEAM") {
                                SeasonCompetitionTeamsScreen(league, season, competition, s, vm)
                            } else {
                                CompetitionParticipantsScreen(league, season, competition, s, vm)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentGroupsTab(s: UiState, competition: CompetitionResponse, vm: AppViewModel) {
    if (s.competitionGroups.isEmpty() && !s.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nessun girone configurato.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(s.competitionGroups) { group ->
                val horizontalScrollState = rememberScrollState()
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = group.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    val rankingMode = competition.rankingMode
                    val showPlayerRanking = rankingMode == "PLAYER" || rankingMode == "BOTH"
                    val showTeamRanking = rankingMode == "TEAM" || rankingMode == "BOTH"

                    if (showTeamRanking) {
                        if (rankingMode == "BOTH") {
                            Text("Classifica Squadre", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        
                        val ranking = s.groupTeamRankings[group.id] ?: emptyList()
                        Column {
                            RankingHeader(isTeamMatch = true, scrollState = horizontalScrollState)
                            ranking.forEachIndexed { index, t ->
                                RankingRow(
                                    position = index + 1, 
                                    title = t.teamName, 
                                    subtitle = null, 
                                    mainValue = t.points.toString(), 
                                    stats = listOf(
                                        "PG" to t.matchesPlayed.toString(),
                                        "V" to t.wins.toString(),
                                        "N" to t.draws.toString(),
                                        "P" to t.losses.toString(),
                                        "GF" to t.goalsFor.toString(),
                                        "GS" to t.goalsAgainst.toString(),
                                        "DR" to t.goalDifference.toString()
                                    ), 
                                    scrollState = horizontalScrollState,
                                    onRowClick = {
                                        vm.loadTeamProfile(t.teamId)
                                        vm.navigateTo(Screen.TeamProfile(t.teamId, t.teamName))
                                    }
                                )
                            }
                        }
                    }

                    if (showPlayerRanking) {
                        if (rankingMode == "BOTH") {
                            Spacer(Modifier.height(12.dp))
                            Text("Classifica Giocatori", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                        
                        val ranking = s.groupPlayerRankings[group.id] ?: emptyList()
                        Column {
                            RankingHeader(isTeamMatch = false, scrollState = horizontalScrollState)
                            ranking.forEachIndexed { index, p ->
                                RankingRow(
                                    position = index + 1, 
                                    title = p.username ?: "Player", 
                                    subtitle = null, 
                                    mainValue = p.rating.toString(), 
                                    stats = listOf(
                                        "PG" to p.matchesPlayed.toString(), 
                                        "GF" to p.goalsFor.toString(), 
                                        "GS" to p.goalsAgainst.toString(), 
                                        "CF" to p.cappottiGiven.toString(), 
                                        "CS" to p.cappottiReceived.toString()
                                    ), 
                                    scrollState = horizontalScrollState,
                                    onRowClick = {
                                        vm.loadPlayerProfile(p.userId)
                                        vm.navigateTo(Screen.PlayerProfile(p.userId, p.username ?: "Giocatore"))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TournamentMatchesTab(
    league: LeagueResponse,
    s: UiState,
    competition: CompetitionResponse,
    vm: AppViewModel,
    onlyUnplayed: Boolean = false,
    onlyPlayed: Boolean = false
) {
    val allMatches = s.seasonMatches
    val matches = remember(allMatches, onlyUnplayed, onlyPlayed) {
        allMatches.filter { match ->
            val isPlayed = match.scoreA != null && match.scoreB != null
            when {
                onlyUnplayed -> !isPlayed && !match.knockoutStage
                onlyPlayed -> isPlayed && !match.knockoutStage
                else -> true
            }
        }
    }

    if (matches.isEmpty() && !s.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val emptyMsg = when {
                onlyUnplayed -> "Tutte le partite di questa fase sono state giocate."
                onlyPlayed -> "Ancora nessuna partita giocata."
                else -> "Nessuna partita programmata."
            }
            Text(emptyMsg, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        // Group by Group
        val groupedByGroup = remember(matches) {
            matches.sortedWith(
                compareBy<MatchResponse> { it.groupName ?: "" }
                    .thenBy { it.roundNumber ?: 0 }
            ).groupBy { it.groupName ?: "" }
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
            groupedByGroup.forEach { (groupName, groupMatches) ->
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ) {
                        Text(
                            text = groupName.uppercase(),
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                // Group groupMatches by RoundHeader
                val groupedByHeader = groupMatches.groupBy { match -> 
                    match.roundNumber?.let { "Giornata $it" } ?: "Altre partite"
                }

                MatchesContent(
                    groupedMatches = groupedByHeader,
                    teams = s.seasonTeams,
                    isAdmin = s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER",
                    rankingType = competition.competitionRankingType,
                    competitionType = "LEAGUE",
                    isCompetitionActive = competition.active ?: (competition.status == "ACTIVE" || competition.status == null),
                    onDeleteMatch = { vm.deleteMatch(competition.id, it) },
                    onUpdateResult = { id, sa, sb -> 
                        vm.updateMatchResult(competition.id, id, sa, sb).invokeOnCompletion {
                            vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
                        }
                    },
                    onEditResult = { id, sa, sb -> 
                        vm.editMatchResult(competition.id, id, sa, sb).invokeOnCompletion {
                            vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
                        }
                    },
                    onMatchClick = { match ->
                        vm.loadMatchComments(match.id)
                        vm.navigateTo(Screen.MatchDetail(match))
                    }
                )

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun TournamentFinalStagePlaceholder() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Fase Finale", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(
                "La fase finale (Quarti, Semifinali, Finale) verrà visualizzata qui dopo la conclusione dei gironi.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
