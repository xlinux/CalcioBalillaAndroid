package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.biliardino.model.*
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun CompetitionStatisticsScreen(
    league: LeagueResponse,
    season: SeasonResponse,
    competition: CompetitionResponse,
    s: UiState,
    vm: AppViewModel
) {
    // This component now acts as a dispatcher based on competition type
    when (competition.type) {
        "LEAGUE" -> LeagueCompetitionView(league, season, competition, s, vm)
        "CUP" -> CupCompetitionView(league, season, competition, s, vm)
        "TOURNAMENT" -> TournamentCompetitionView(league, season, competition, s, vm)
        "PLAYOFF" -> PlayoffCompetitionView(league, season, competition, s, vm)
        else -> {
            // Fallback for unknown types
            Box(Modifier.fillMaxSize()) {
                Text("Tipo competizione non supportato: ${competition.type}")
            }
        }
    }
}
