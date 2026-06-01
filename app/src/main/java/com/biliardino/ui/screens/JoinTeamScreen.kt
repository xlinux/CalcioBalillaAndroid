package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
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
import com.biliardino.model.CompetitionResponse
import com.biliardino.model.CreateTeamRequest
import com.biliardino.model.LeagueResponse
import com.biliardino.model.SeasonResponse
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun JoinTeamScreen(
    league: LeagueResponse,
    season: SeasonResponse,
    competition: CompetitionResponse,
    s: UiState,
    vm: AppViewModel
) {
    var teamName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var tabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Partecipa a ${competition.name}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Questa competizione è a squadre. Crea una nuova squadra o unisciti a una esistente.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        TabRow(selectedTabIndex = tabIndex) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = { Text("Crea Squadra") },
                icon = { Icon(Icons.Default.Groups, contentDescription = null) }
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = { Text("Usa Codice") },
                icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) }
            )
        }

        Spacer(Modifier.height(24.dp))

        if (tabIndex == 0) {
            // Crea Squadra
            OutlinedTextField(
                value = teamName,
                onValueChange = { teamName = it },
                label = { Text("Nome Squadra") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    vm.createTeam(
                        competitionId = competition.id,
                        request = CreateTeamRequest(name = teamName, playerAId = null, playerBId = null),
                        onSuccess = {
                            vm.selectCompetition(league, season, competition)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = teamName.isNotBlank() && !s.loading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (s.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Crea e Partecipa")
                }
            }
        } else {
            // Unisciti con codice
            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it },
                label = { Text("Codice Invito") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { vm.joinTeam(inviteCode) },
                modifier = Modifier.fillMaxWidth(),
                enabled = inviteCode.isNotBlank() && !s.loading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (s.loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Entra nella Squadra")
                }
            }
        }
    }
}
