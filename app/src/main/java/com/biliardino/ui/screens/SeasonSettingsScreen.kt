package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biliardino.model.CompetitionResponse
import com.biliardino.model.LeagueResponse
import com.biliardino.model.SeasonResponse
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun SeasonSettingsScreen(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse?, s: UiState, vm: AppViewModel) {
    var showCloseCompetitionConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (competition != null) {
            Text(
                text = "Dettagli Competizione",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            InfoCard(
                title = "Informazioni Competizione",
                items = listOf(
                    "Nome" to competition.name,
                    "Tipo" to if (competition.type == "LEAGUE") "Campionato" else "Torneo",
                    "Stato" to if (competition.status == "ACTIVE" || competition.status == null) "Attiva" else "Conclusa"
                )
            )
            
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }

        Text(
            text = "Dettagli Stagione",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        InfoCard(
            title = "Informazioni Generali",
            items = listOf(
                "Nome" to season.name,
                "Data Inizio" to season.startDate,
                "Data Fine" to season.endDate,
                "Stato" to if (season.active == true) "Attiva" else "Conclusa"
            )
        )

        InfoCard(
            title = "Regolamento Punteggio",
            items = listOf(
                "Punteggio Vittoria" to "${season.targetScore} punti",
                "Abilita Cappotto" to if (season.cappottoEnabled) "Sì" else "No",
                "Bonus Cappotto" to if (season.cappottoEnabled) "+${season.cappottoBonus} punti" else "N/A"
            )
        )

        InfoCard(
            title = "Permessi e Restrizioni",
            items = listOf(
                "Iscrizione post-inizio" to if (season.allowJoinAfterStart) "Permessa" else "Bloccata",
                "Partite post-fine" to if (season.allowMatchesAfterEnd) "Permesse" else "Bloccate"
            )
        )

        // Close Competition Action at the end
        if (competition != null && (competition.status == "ACTIVE" || competition.status == null)) {
            val isAdminOrOwner = s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER"
            if (isAdminOrOwner) {
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = { vm.recalculateCompetition(competition.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "RICALCOLA PUNTI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Rielabora tutte le partite della competizione per aggiornare correttamente le classifiche.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { showCloseCompetitionConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "CHIUDI COMPETIZIONE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Attenzione: chiudere la competizione è un'operazione irreversibile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showCloseCompetitionConfirmation && competition != null) {
        AlertDialog(
            onDismissRequest = { showCloseCompetitionConfirmation = false },
            title = { Text("Chiudere la Competizione?") },
            text = { Text("Sei sicuro di voler chiudere definitivamente la competizione '${competition.name}'? Questa operazione non può essere annullata.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.closeCompetition(league, season, competition)
                        showCloseCompetitionConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("SÌ, CHIUDI")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseCompetitionConfirmation = false }) {
                    Text("ANNULLA")
                }
            }
        )
    }
}

@Composable
fun InfoCard(title: String, items: List<Pair<String, String>>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            items.forEach { (label, value) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
