package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.biliardino.model.*
import com.biliardino.ui.Screen
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun SeasonCompetitionTeamsScreen(
    league: LeagueResponse,
    season: SeasonResponse,
    competition: CompetitionResponse,
    s: UiState,
    vm: AppViewModel
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (s.seasonTeams.isEmpty() && !s.loading) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Groups, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Nessuna squadra iscritta a questa competizione.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Squadre Iscritte",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(s.seasonTeams) { team ->
                        TeamListItem(
                            team = team,
                            matchType = competition.matchType,
                            onTeamClick = {
                                vm.loadTeamDetailData(competition.id, team.id)
                            },
                            onPlayerAClick = {
                                team.playerAId?.let {
                                    vm.loadPlayerProfile(it)
                                    vm.navigateTo(Screen.PlayerProfile(it, team.playerAUsername ?: "Giocatore"))
                                }
                            },
                            onPlayerBClick = {
                                team.playerBId?.let {
                                    vm.loadPlayerProfile(it)
                                    vm.navigateTo(Screen.PlayerProfile(it, team.playerBUsername ?: "Giocatore"))
                                }
                            }
                        )
                    }
                }
            }
        }

        if ((s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER") && competition.registrationOpen) {
            FloatingActionButton(
                onClick = {
                    vm.navigateTo(Screen.CreateTeam(league, season, competition))
                },
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
                    Icon(Icons.Default.GroupAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Crea Squadra")
                }
            }
        }
    }
}
