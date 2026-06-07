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
import com.biliardino.ui.components.TournamentBracketView
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CupCompetitionView(
    league: LeagueResponse,
    season: SeasonResponse,
    competition: CompetitionResponse,
    s: UiState,
    vm: AppViewModel
) {
    var searchQuery by remember { mutableStateOf("") }

    val tabs = listOf("Tabellone", "Chat", "Squadre")

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

        if (s.currentCompetitionMode == "MAIN" && selectedTab == 2) {
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
                        "Tabellone" -> TournamentBracketView(
                            matches = s.seasonMatches,
                            onMatchClick = { match ->
                                vm.loadMatchComments(match.id)
                                vm.navigateTo(Screen.MatchDetail(match))
                            }
                        )
                        "Chat" -> CompetitionChatTab(competition.id, s, vm)
                        "Squadre" -> {
                            val filteredTeams = s.seasonTeams
                                .filter { it.name.contains(searchQuery, ignoreCase = true) }
                                .sortedBy { it.name }
                            SeasonTeamsScreen(league, season, competition, s.copy(seasonTeams = filteredTeams), vm)
                        }
                    }
                }
            }
        }
    }
}
