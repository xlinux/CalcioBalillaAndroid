package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biliardino.model.*
import com.biliardino.ui.Screen
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueCompetitionView(
    league: LeagueResponse,
    season: SeasonResponse,
    competition: CompetitionResponse,
    s: UiState,
    vm: AppViewModel
) {
    var searchQuery by remember { mutableStateOf("") }

    val isTeamMatch = competition.matchType == "TEAM"
    val tabs = if (isTeamMatch) {
        listOf("Classifica", "Chat", "Squadre")
    } else {
        listOf("Classifica", "Giocatori", "Chat", "Squadre")
    }

    val selectedTab = if (s.currentCompetitionTab >= tabs.size) 0 else s.currentCompetitionTab

    LaunchedEffect(Unit) {
        vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
    }

    LaunchedEffect(selectedTab) {
        val chatTabIndex = tabs.indexOf("Chat")
        if (selectedTab == chatTabIndex && chatTabIndex != -1) {
            vm.loadCompetitionComments(competition.id)
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (s.currentCompetitionMode == "MAIN") {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { 
                            vm.updateCompetitionTab(index)
                            searchQuery = "" 
                        },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Bold) }
                    )
                }
            }
        }

        val isSearchVisible = s.currentCompetitionMode == "MAIN" && if (isTeamMatch) selectedTab == 2 else (selectedTab == 1 || selectedTab == 3)

        if (isSearchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                placeholder = { Text("Cerca...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }

        Box(Modifier.weight(1f)) {
            when (s.currentCompetitionMode) {
                "MATCHES" -> CompetitionMatchesScreen(league, season, competition, s, vm)
                "HISTORY" -> TournamentMatchesTab(league, s, competition, vm, onlyPlayed = true)
                else -> {
                    val tabTitle = tabs[selectedTab]
                    when (tabTitle) {
                        "Classifica" -> Column {
                            var rankingTab by remember { mutableIntStateOf(0) }
                            val showPlayerRanking = competition.rankingMode != "TEAM"
                            val showTeamRanking = competition.rankingMode != "PLAYER"
                            
                            if (showPlayerRanking && showTeamRanking) {
                                SecondaryTabRow(selectedTabIndex = rankingTab, containerColor = MaterialTheme.colorScheme.surface) {
                                    Tab(selected = rankingTab == 0, onClick = { rankingTab = 0 }, text = { Text("GIOCATORI", style = MaterialTheme.typography.labelLarge) })
                                    Tab(selected = rankingTab == 1, onClick = { rankingTab = 1 }, text = { Text("SQUADRE", style = MaterialTheme.typography.labelLarge) })
                                }
                            }
                            when {
                                showPlayerRanking && showTeamRanking && rankingTab == 0 -> PlayerRankingList(s.playerRankings, vm)
                                showPlayerRanking && showTeamRanking -> TeamRankingList(s.teamRankings, competition, vm)
                                showPlayerRanking -> PlayerRankingList(s.playerRankings, vm)
                                else -> TeamRankingList(s.teamRankings, competition, vm)
                            }
                        }
                        "Giocatori" -> {
                            val filteredUsers = s.seasonUsers
                                .filter { it.username.contains(searchQuery, ignoreCase = true) || (it.email?.contains(searchQuery, ignoreCase = true) == true) }
                                .sortedByDescending { it.rating }
                            UserList(filteredUsers) { user ->
                                vm.loadPlayerProfile(user.userId)
                                vm.navigateTo(Screen.PlayerProfile(user.userId, user.username))
                            }
                        }
                        "Chat" -> CompetitionChatTab(competition.id, s, vm)
                        "Squadre" -> {
                            val filteredTeams = s.seasonTeams
                                .filter { it.name.contains(searchQuery, ignoreCase = true) || (it.playerAUsername?.contains(searchQuery, ignoreCase = true) == true) || (it.playerBUsername?.contains(searchQuery, ignoreCase = true) == true) }
                                .sortedByDescending { it.rating ?: 0 }
                            SeasonTeamsScreen(league, season, competition, s.copy(seasonTeams = filteredTeams), vm)
                        }
                    }
                }
            }
        }
    }
}
