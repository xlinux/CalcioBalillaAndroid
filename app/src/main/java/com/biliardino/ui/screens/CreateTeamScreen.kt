package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.biliardino.model.*
import com.biliardino.ui.Screen
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTeamScreen(
    league: LeagueResponse,
    season: SeasonResponse,
    competition: CompetitionResponse,
    s: UiState,
    vm: AppViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuova Squadra") }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            CreateTeamView(
                users = s.seasonUsers,
                matchType = competition.matchType,
                onConfirm = { request ->
                    vm.createTeam(competition.id, request) {
                        vm.navigateTo(Screen.CompetitionStatistics(league, season, competition))
                    }
                },
                onCancel = {
                    vm.navigateTo(Screen.CompetitionStatistics(league, season, competition))
                }
            )
        }
    }
}
