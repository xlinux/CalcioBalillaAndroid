package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biliardino.model.CreateSeasonRequest
import com.biliardino.model.LeagueResponse
import com.biliardino.model.SeasonResponse
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState
import java.time.LocalDate

@Composable
fun LeagueSeasonsScreen(league: LeagueResponse, s: UiState, vm: AppViewModel) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(s.seasons) { season ->
                SeasonCard(season = season, onClick = { vm.selectSeason(league, season) })
            }
        }
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier.padding(16.dp).align(Alignment.End)
        ) { Icon(Icons.Default.Add, "Crea Stagione") }
    }

    if (showCreateDialog) {
        CreateSeasonDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { request ->
                vm.createSeason(league.id, request)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun SeasonCard(season: SeasonResponse, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = season.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (season.active == true) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            "ATTIVA",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Obiettivo: ${season.targetScore} punti",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Periodo: ${season.startDate} - ${season.endDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CreateSeasonDialog(onDismiss: () -> Unit, onConfirm: (CreateSeasonRequest) -> Unit) {
    var name by remember { mutableStateOf("") }
    var targetScore by remember { mutableStateOf("10") }
    var cappottoEnabled by remember { mutableStateOf(true) }
    var cappottoBonus by remember { mutableStateOf("2") }
    var allowJoinAfterStart by remember { mutableStateOf(true) }
    var allowMatchesAfterEnd by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova Stagione") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Stagione") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = targetScore,
                    onValueChange = { targetScore = it },
                    label = { Text("Punteggio Vittoria") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = cappottoEnabled, onCheckedChange = { cappottoEnabled = it })
                    Text("Abilita Cappotto")
                }

                if (cappottoEnabled) {
                    OutlinedTextField(
                        value = cappottoBonus,
                        onValueChange = { cappottoBonus = it },
                        label = { Text("Valore Bonus Cappotto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allowJoinAfterStart, onCheckedChange = { allowJoinAfterStart = it })
                    Text("Permetti iscrizione dopo inizio")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allowMatchesAfterEnd, onCheckedChange = { allowMatchesAfterEnd = it })
                    Text("Permetti partite dopo fine")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    CreateSeasonRequest(
                        name = name,
                        startDate = LocalDate.now().toString(),
                        endDate = LocalDate.now().plusMonths(3).toString(),
                        targetScore = targetScore.toIntOrNull() ?: 10,
                        cappottoEnabled = cappottoEnabled,
                        cappottoBonus = cappottoBonus.toIntOrNull() ?: 2,
                        allowJoinAfterStart = allowJoinAfterStart,
                        allowMatchesAfterEnd = allowMatchesAfterEnd
                    )
                )
            }) { Text("Crea") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}
