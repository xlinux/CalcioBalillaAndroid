package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var showCloseRegistrationConfirmation by remember { mutableStateOf(false) }
    var showDeleteCompetitionConfirmation by remember { mutableStateOf(false) }
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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (competition != null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = competition.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configurazione e strumenti di gestione",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (competition.active == false && (competition.winnerTeamId != null || competition.winnerUserId != null)) {
                val winnerName = when {
                    competition.winnerTeamId != null -> s.seasonTeams.find { it.id == competition.winnerTeamId }?.name
                    competition.winnerUserId != null -> s.seasonUsers.find { it.userId == competition.winnerUserId }?.username
                    else -> null
                }
                
                if (winnerName != null) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏆 Vincitore", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(8.dp))
                            Text(winnerName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            InfoCard(
                title = "Informazioni Generali",
                description = "Dettagli principali della competizione e stato attuale delle iscrizioni.",
                items = mutableListOf(
                    "Nome" to competition.name,
                    "Sport" to (competition.sportName ?: "Calcio Balilla"),
                    "Tipo" to if (competition.type == "LEAGUE") "Campionato (Girone)" else "Torneo (Eliminazione)",
                    "Data Inizio" to DateUtils.formatDate(competition.startDate),
                    "Stato" to (if (competition.status == "ACTIVE" || competition.status == null) "Attiva" else "Conclusa"),
                    "Iscrizioni" to (if (competition.registrationOpen) "🟢 Aperte" else "🔒 Chiuse")
                )
            )

            InfoCard(
                title = "Regole e Formato",
                description = "Parametri di gioco e sistema di punteggio applicato ai match.",
                items = mutableListOf<Pair<String, String>>().apply {
                    add("Modalità Match" to when(competition.matchType) {
                        "SINGLE" -> "Singolo (1vs1)"
                        "DOUBLE" -> "Doppio (2vs2)"
                        "TEAM" -> "Squadra"
                        else -> competition.matchType
                    })
                    add("Formato" to if (competition.matchFormat == "POINTS") "A Punti" else if (competition.matchFormat == "GOALS") "Goal" else "A Set")
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
                title = "Logistica e Ranking",
                description = "Impostazioni sulla generazione degli incontri e calcolo delle classifiche.",
                items = mutableListOf(
                    "Creazione Match" to if (competition.matchCreationMode == "FREE") "Libera (gli utenti inseriscono i risultati)" else "Programmata (admin genera calendario)",
                    "Calendario" to if (competition.calendarGenerationMode == "ROUNDS") "A giornate" else "Sequenziale",
                    "Tipo Ranking" to when(competition.competitionRankingType) {
                        "POINTS" -> "Punti Classifica"
                        "ELO" -> "Sistema ELO"
                        "WIN_RATE" -> "Percentuale Vittorie"
                        else -> competition.competitionRankingType
                    },
                    "Visibilità" to when(competition.rankingMode) {
                        "PLAYER" -> "Solo Classifica Giocatori"
                        "TEAM" -> "Solo Classifica Squadre"
                        else -> "Entrambe le Classifiche"
                    }
                )
            )
            
            val isAdminOrOwner = s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER"
            val typeName = if (league.leagueType == "CLUB") "Circolo" else "Lega"
            
            if (isAdminOrOwner) {
                Text(
                    "Azioni di Gestione",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                AdminActionCard(
                    title = "Membri $typeName e Squadre",
                    description = "Aggiungi o rimuovi giocatori e gestisci le formazioni delle squadre.",
                    buttonText = "GESTISCI",
                    onClick = {
                        vm.navigateTo(Screen.CompetitionParticipants(league, season, competition))
                        vm.loadCompetitionPlayers(competition.id)
                    },
                    color = MaterialTheme.colorScheme.secondary
                )

                if (competition.matchCreationMode == "SCHEDULED" && competition.type == "LEAGUE") {
                    val canGenerate = competition.active != false &&
                            s.seasonTeams.size >= 2 &&
                            s.seasonMatches.isEmpty()
                    
                    AdminActionCard(
                        title = "Generazione Calendario",
                        description = "Crea automaticamente tutti gli incontri basandoti sulle squadre iscritte. Questa operazione richiede almeno 2 squadre.",
                        buttonText = "GENERA CALENDARIO",
                        onClick = { showGenerateDialog = true },
                        enabled = canGenerate,
                        statusText = if (s.seasonMatches.isNotEmpty()) "Calendario già generato." else if (s.seasonTeams.size < 2) "Servono almeno 2 squadre." else null
                    )
                }

                if (competition.type == "CUP") {
                    val validEntryCounts = listOf(4, 8, 16, 32)
                    val teamsCount = s.seasonTeams.size
                    val canGenerateBracket = competition.active != false &&
                            teamsCount in validEntryCounts &&
                            s.seasonMatches.isEmpty()

                    var showGenerateBracketDialog by remember { mutableStateOf(false) }

                    AdminActionCard(
                        title = "Generazione Tabellone",
                        description = "Crea il tabellone del torneo a eliminazione diretta. Richiede esattamente 4, 8, 16 o 32 squadre.",
                        buttonText = "GENERA TABELLONE",
                        onClick = { showGenerateBracketDialog = true },
                        enabled = canGenerateBracket,
                        statusText = if (s.seasonMatches.isNotEmpty()) "Tabellone già generato." else null
                    )

                    CupValidationSection(
                        teams = s.seasonTeams,
                        validCounts = validEntryCounts,
                        isAlreadyGenerated = s.seasonMatches.isNotEmpty()
                    )

                    if (showGenerateBracketDialog) {
                        AlertDialog(
                            onDismissRequest = { showGenerateBracketDialog = false },
                            title = { Text("Genera Tabellone") },
                            text = { Text("Sei sicuro di voler generare il tabellone del torneo? Verranno creati tutti gli incontri a eliminazione diretta.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    vm.generateBracket(competition.id)
                                    showGenerateBracketDialog = false
                                }) {
                                    Text("GENERA")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showGenerateBracketDialog = false }) {
                                    Text("ANNULLA")
                                }
                            }
                        )
                    }
                }

                if (competition.status == "ACTIVE" || competition.status == null) {
                    if (competition.registrationOpen) {
                        AdminActionCard(
                            title = "Chiusura Iscrizioni",
                            description = "Disabilita la possibilità per nuovi giocatori di unirsi o creare squadre. Usa questa azione prima di generare il calendario.",
                            buttonText = "CHIUDI ISCRIZIONI",
                            onClick = { showCloseRegistrationConfirmation = true },
                            color = Color(0xFFF57C00)
                        )
                    }

                    AdminActionCard(
                        title = "Manutenzione Punteggi",
                        description = "Ricalcola tutti i punti della classifica basandoti sullo storico dei match. Utile in caso di anomalie nei punteggi.",
                        buttonText = "RICALCOLA PUNTI",
                        onClick = { vm.recalculateCompetition(competition.id) },
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    AdminActionCard(
                        title = "Termina Competizione",
                        description = "Chiude definitivamente la competizione. Non sarà più possibile aggiungere match o modificare risultati.",
                        buttonText = "CHIUDI COMPETIZIONE",
                        onClick = { showCloseCompetitionConfirmation = true },
                        color = MaterialTheme.colorScheme.error
                    )
                }

                AdminActionCard(
                    title = "Elimina Competizione",
                    description = "Elimina definitivamente la competizione e tutti i dati collegati (partite, squadre, statistiche). Questa operazione non può essere annullata.",
                    buttonText = "ELIMINA COMPETIZIONE",
                    onClick = { showDeleteCompetitionConfirmation = true },
                    color = MaterialTheme.colorScheme.error
                )
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

    if (showCloseRegistrationConfirmation && competition != null) {
        AlertDialog(
            onDismissRequest = { showCloseRegistrationConfirmation = false },
            title = { Text("Chiudere le Iscrizioni?") },
            text = { Text("Sei sicuro di voler chiudere le iscrizioni per '${competition.name}'? Una volta chiuse, non sarà più possibile aggiungere nuovi partecipanti o squadre.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.closeRegistration(league, season, competition)
                        showCloseRegistrationConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF57C00))
                ) {
                    Text("SÌ, CHIUDI ISCRIZIONI")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseRegistrationConfirmation = false }) {
                    Text("ANNULLA")
                }
            }
        )
    }

    if (showDeleteCompetitionConfirmation && competition != null) {
        AlertDialog(
            onDismissRequest = { showDeleteCompetitionConfirmation = false },
            title = { Text("Eliminare la Competizione?") },
            text = { Text("Sei sicuro di voler eliminare questa competizione? Verranno eliminati anche partite, squadre, risultati e statistiche collegate. L'operazione non può essere annullata.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteCompetition(league, season, competition.id)
                        showDeleteCompetitionConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ELIMINA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCompetitionConfirmation = false }) {
                    Text("ANNULLA")
                }
            }
        )
    }
}

@Composable
fun CupValidationSection(
    teams: List<com.biliardino.model.TeamResponse>,
    validCounts: List<Int>,
    isAlreadyGenerated: Boolean
) {
    if (isAlreadyGenerated) return

    val teamsCount = teams.size
    val isValid = teamsCount in validCounts

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 1. Numero squadre iscritte
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Squadre iscritte: ",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$teamsCount",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // 2. Stato validazione e 4. Suggerimento
            if (isValid) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✓", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Numero valido per generare il tabellone",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("⚠️", modifier = Modifier.padding(top = 2.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Numero non valido per una coppa a eliminazione diretta. Sono supportati: ${validCounts.joinToString(", ")} squadre.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    val nextValid = validCounts.firstOrNull { it > teamsCount }
                    val prevValid = validCounts.lastOrNull { it < teamsCount }

                    if (nextValid != null || prevValid != null) {
                        Text(
                            "Per continuare:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            prevValid?.let {
                                Text(
                                    "• rimuovere ${teamsCount - it} squadre per arrivare a $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            nextValid?.let {
                                Text(
                                    "• aggiungere ${it - teamsCount} squadre per arrivare a $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // 3. Elenco squadre partecipanti
            if (teams.isNotEmpty()) {
                HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    "Squadre partecipanti:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    teams.forEach { team ->
                        Text(
                            "• ${team.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCard(title: String, description: String? = null, items: List<Pair<String, String>>) {
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
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            items.forEachIndexed { index, (label, value) ->
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

@Composable
fun AdminActionCard(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    statusText: String? = null
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = color),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }

            if (statusText != null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
