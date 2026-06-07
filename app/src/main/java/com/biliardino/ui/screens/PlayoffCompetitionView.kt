package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
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
fun PlayoffCompetitionView(
    league: LeagueResponse,
    season: SeasonResponse,
    competition: CompetitionResponse,
    s: UiState,
    vm: AppViewModel
) {
    val tabs = listOf("Tabellone Playoff", "Chat")

    val selectedTab = if (s.currentCompetitionTab >= tabs.size) 0 else s.currentCompetitionTab

    LaunchedEffect(Unit) {
        vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            vm.loadCompetitionComments(competition.id)
        }
    }

    Column(Modifier.fillMaxSize()) {
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

        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> TournamentBracketView(
                    matches = s.seasonMatches,
                    onMatchClick = { match ->
                        vm.loadMatchComments(match.id)
                        vm.navigateTo(Screen.MatchDetail(match))
                    }
                )
                1 -> CompetitionChatTab(competition.id, s, vm)
            }
        }
    }
}
