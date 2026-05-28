package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biliardino.model.*
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun CompetitionMatchesScreen(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse, s: UiState, vm: AppViewModel) {
    if (competition.status == "CONCLUDED") {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Competizione Conclusa",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Non è più possibile registrare nuovi risultati per questa competizione.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    } else {
        MatchEntryView(s.seasonTeams) { request ->
            vm.createMatch(competition.id, request)
        }
    }
}

@Composable
fun MatchEntryView(teams: List<TeamResponse>, onConfirm: (CreateDoubleMatchRequest) -> Unit) {
    var teamA by remember { mutableStateOf<TeamResponse?>(null) }
    var teamB by remember { mutableStateOf<TeamResponse?>(null) }
    var scoreA by remember { mutableIntStateOf(0) }
    var scoreB by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Registra Risultato", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SQUADRA A", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                SearchableTeamDropdown(
                    teams = teams.filter { team ->
                        teamB == null || (
                            team.id != teamB?.id &&
                            !haveSharedPlayers(team, teamB)
                        )
                    },
                    selectedTeam = teamA,
                    onTeamSelected = { teamA = it }
                )
                teamA?.let {
                    Text(
                        text = "${it.playerAUsername} & ${it.playerBUsername}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SQUADRA B", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                SearchableTeamDropdown(
                    teams = teams.filter { team ->
                        teamA == null || (
                            team.id != teamA?.id &&
                            !haveSharedPlayers(team, teamA)
                        )
                    },
                    selectedTeam = teamB,
                    onTeamSelected = { teamB = it }
                )
                teamB?.let {
                    Text(
                        text = "${it.playerAUsername} & ${it.playerBUsername}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        HorizontalDivider()

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScorePicker(scoreA, { scoreA = it })
            Text("-", style = MaterialTheme.typography.displaySmall)
            ScorePicker(scoreB, { scoreB = it })
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
                scoreA = 0
                scoreB = 0
                teamA = null
                teamB = null
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("SALVA RISULTATO", style = MaterialTheme.typography.titleMedium)
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
fun SearchableTeamDropdown(
    teams: List<TeamResponse>,
    selectedTeam: TeamResponse?,
    onTeamSelected: (TeamResponse) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredTeams = remember(searchQuery, teams) {
        if (searchQuery.isBlank()) teams
        else teams.filter { team ->
            team.name.contains(searchQuery, ignoreCase = true) ||
            (team.playerAUsername?.contains(searchQuery, ignoreCase = true) ?: false) ||
            (team.playerBUsername?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    }

    Box {
        OutlinedButton(
            onClick = { expanded = true; searchQuery = "" },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = selectedTeam?.name ?: "Scegli...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 400.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Cerca squadra o giocatore...") },
                leadingIcon = { Icon(Icons.Default.List, null) },
                singleLine = true
            )
            
            if (filteredTeams.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nessun risultato") },
                    onClick = { },
                    enabled = false
                )
            } else {
                filteredTeams.forEach { team ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(team.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${team.playerAUsername} & ${team.playerBUsername}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        onClick = {
                            onTeamSelected(team)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScorePicker(value: Int, onValueChange: (Int) -> Unit) {
    val focusManager = LocalFocusManager.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        IconButton(onClick = { onValueChange(value + 1) }) {
            Icon(Icons.Default.Add, "Incrementa")
        }
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onValueChange(it) }
            },
            textStyle = MaterialTheme.typography.displaySmall.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
        IconButton(onClick = { if (value > 0) onValueChange(value - 1) }) {
            // Using a standard remove/minus icon would be better, but keeping the style for now with fixed icon
            Icon(Icons.Default.Add, "Decrementa", modifier = Modifier.rotate(45f))
        }
    }
}
