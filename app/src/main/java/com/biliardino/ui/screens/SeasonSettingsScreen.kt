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
import com.biliardino.ui.Screen
import com.biliardino.util.DateUtils
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun SeasonSettingsScreen(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse?, s: UiState, vm: AppViewModel) {
    var showCloseCompetitionConfirmation by remember { mutableStateOf(false) }
    var showGenerateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.loadSeasonStatsData(league.id, season.id, competition?.id)
        competition?.id?.let { vm.loadCompetitionMatches(it) }
    }

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
                title = "Informazioni Generali",
                items = mutableListOf(
                    "Nome" to competition.name,
                    "Sport" to (competition.sportName ?: "Calcio Balilla"),
                    "Tipo" to if (competition.type == "LEAGUE") "Campionato" else "Torneo",
                    "Modalità Creazione" to competition.matchCreationMode,
                    "Generazione Calendario" to if (competition.calendarGenerationMode == "ROUNDS") "A giornate" else "Sequenziale",
                    "Modalità Match" to when(competition.matchType) {
                        "SINGLE" -> "Singolo (1vs1)"
                        "DOUBLE" -> "Doppio (2vs2)"
                        "TEAM" -> "Squadra"
                        else -> competition.matchType
                    },
                    "Data Inizio" to DateUtils.formatDate(competition.startDate),
                    "Data Fine" to DateUtils.formatDate(competition.endDate),
                    "Stato" to if (competition.status == "ACTIVE" || competition.status == null) "Attiva" else "Conclusa"
                )
            )

            InfoCard(
                title = "Regole del Match",
                items = mutableListOf<Pair<String, String>>().apply {
                    add("Formato" to if (competition.matchFormat == "POINTS") "A Punti" else "A Set")
                    if (competition.matchFormat == "POINTS" && competition.useTargetScore) {
                        add("Punteggio Target" to "${competition.targetScore} punti")
                        if (competition.cappottoEnabled) {
                            add("Bonus Cappotto" to "+${competition.cappottoBonusPoints} punti")
                        }
                    }
                    add("Andata e Ritorno" to if (competition.homeAndAway) "Sì" else "No")
                }
            )

            InfoCard(
                title = "Sistema Classifica",
                items = mutableListOf(
                    "Tipo Ranking" to when(competition.competitionRankingType) {
                        "POINTS" -> "Punti Classifica"
                        "ELO" -> "Sistema ELO"
                        "WIN_RATE" -> "Percentuale Vittorie"
                        else -> competition.competitionRankingType
                    },
                    "Visibilità" to when(competition.rankingMode) {
                        "PLAYER" -> "Solo Giocatori"
                        "TEAM" -> "Solo Squadre"
                        else -> "Giocatori e Squadre"
                    }
                ).apply {
                    if (competition.competitionRankingType == "POINTS") {
                        add("Punti Vittoria" to "${competition.winPoints} pt")
                        if (competition.allowDraw) {
                            add("Punti Pareggio" to "${competition.drawPoints} pt")
                        }
                        add("Punti Sconfitta" to "${competition.lossPoints} pt")
                    }
                }
            )
            
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            val isAdminOrOwner = s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER"
            if (isAdminOrOwner) {
                Button(
                    onClick = {
                        vm.navigateTo(Screen.CompetitionParticipants(league, season, competition))
                        vm.loadCompetitionPlayers(competition.id)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "GESTISCI PARTECIPANTI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (competition.matchCreationMode == "SCHEDULED" && competition.type == "LEAGUE") {
                    Spacer(Modifier.height(8.dp))
                    val canGenerate = competition.active != false &&
                            s.seasonTeams.size >= 2 &&
                            s.seasonMatches.isEmpty()

                    Button(
                        onClick = { showGenerateDialog = true },
                        enabled = canGenerate,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            "GENERA CALENDARIO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (s.seasonMatches.isNotEmpty()) {
                        Text(
                            "Calendario già generato.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else if (s.seasonTeams.size < 2) {
                        Text(
                            "Servono almeno 2 squadre.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

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
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showGenerateDialog && competition != null) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("Genera Calendario") },
            text = { Text("Sei sicuro di voler generare il calendario delle partite? Questa azione creerà tutti gli incontri previsti e non potrà essere annullata.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.generateCalendar(competition.id)
                    showGenerateDialog = false
                }) {
                    Text("GENERA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text("ANNULLA")
                }
            }
        )
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
