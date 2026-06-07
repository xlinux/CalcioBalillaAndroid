package com.biliardino.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biliardino.model.CompetitionResponse
import com.biliardino.model.CompetitionTemplateResponse
import com.biliardino.model.CreateCompetitionRequest
import com.biliardino.model.LeagueResponse
import com.biliardino.model.SeasonResponse
import com.biliardino.model.SportResponse
import com.biliardino.util.DateUtils
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCompetitionScreen(
    league: LeagueResponse,
    season: SeasonResponse,
    s: UiState,
    vm: AppViewModel
) {
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("LEAGUE") }
    var selectedRankingMode by remember { mutableStateOf("BOTH") }
    var selectedMatchType by remember { mutableStateOf("SINGLE") }
    var selectedCompRankingType by remember { mutableStateOf("POINTS") }
    var selectedMatchFormat by remember { mutableStateOf("POINTS") }
    var winByTwo by remember { mutableStateOf(false) }
    var matchCreationMode by remember { mutableStateOf("FREE") }
    var calendarGenerationMode by remember { mutableStateOf("ROUNDS") }
    var selectedSportId by remember { mutableStateOf<Long?>(null) }
    var selectedTemplateSportId by remember { mutableStateOf<Long?>(null) }
    var tournamentFormat by remember { mutableStateOf<String?>("GROUPS_THEN_SINGLE_ELIMINATION") }
    var joinCreator by remember { mutableStateOf(false) }
    var useTargetScore by remember { mutableStateOf(false) }
    var targetScore by remember { mutableStateOf(10) }
    var allowDraw by remember { mutableStateOf(false) }
    var winPoints by remember { mutableStateOf(3) }
    var drawPoints by remember { mutableStateOf(0) }
    var lossPoints by remember { mutableStateOf(0) }
    var cappottoEnabled by remember { mutableStateOf(false) }
    var cappottoBonusPoints by remember { mutableStateOf(0) }
    var homeAndAway by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(DateUtils.now()) }
    var endDate by remember { mutableStateOf(DateUtils.monthsFromNow(3)) }
    var copyParticipants by remember { mutableStateOf(false) }
    var copyTeams by remember { mutableStateOf(false) }
    var copyFromCompetitionId by remember { mutableStateOf<Long?>(null) }

    fun applyTemplate(template: CompetitionTemplateResponse) {
        selectedTemplateSportId = template.sportId
        name = template.competitionName
        selectedSportId = template.sportId
        selectedType = template.type
        selectedRankingMode = template.rankingMode
        selectedMatchType = template.matchType
        selectedCompRankingType = template.competitionRankingType
        selectedMatchFormat = template.matchFormat
        winByTwo = false // Default
        matchCreationMode = if (template.type == "LEAGUE") "SCHEDULED" else "FREE"
        targetScore = (template.targetScore ?: targetScore).coerceAtLeast(1)
        useTargetScore = template.useTargetScore
        allowDraw = template.allowDraw
        winPoints = template.winPoints
        drawPoints = template.drawPoints
        lossPoints = template.lossPoints
        cappottoEnabled = template.cappottoEnabled
        cappottoBonusPoints = template.cappottoBonusPoints
        homeAndAway = template.homeAndAway
        
        // Forza ROUNDS se il template è un campionato andata/ritorno
        if (template.type == "LEAGUE" && template.homeAndAway) {
            calendarGenerationMode = "ROUNDS"
        } else {
            calendarGenerationMode = template.calendarGenerationMode
        }
    }

    LaunchedEffect(selectedType, homeAndAway) {
        if (selectedType == "LEAGUE" && homeAndAway) {
            calendarGenerationMode = "ROUNDS"
        }
    }

    LaunchedEffect(Unit) {
        vm.loadCompetitionTemplates()
        vm.loadSports()
    }

    LaunchedEffect(copyParticipants, copyTeams, s.competitions) {
        val shouldCopy = copyParticipants || copyTeams
        if (!shouldCopy) {
            copyFromCompetitionId = null
        } else if (copyFromCompetitionId == null || s.competitions.none { it.id == copyFromCompetitionId }) {
            copyFromCompetitionId = s.competitions.firstOrNull()?.id
        }
    }

    val shouldCopy = copyParticipants || copyTeams
    val validDates = DateUtils.toIsoDate(endDate) >= DateUtils.toIsoDate(startDate)
    val canCreate = name.trim().isNotEmpty() &&
        selectedSportId != null &&
        validDates &&
        (!shouldCopy || copyFromCompetitionId != null) &&
        !s.loading
    val canContinue = when (currentStep) {
        0 -> name.trim().isNotEmpty() && selectedSportId != null
        2 -> validDates && (!shouldCopy || copyFromCompetitionId != null)
        else -> true
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        CompetitionCreationProgress(currentStep)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (currentStep) {
                0 -> CreationSectionCard("Dettagli") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome competizione") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            OptionRow(
                label = "Tipo",
                options = listOf("LEAGUE" to "Campionato", "CUP" to "Torneo"),
                selected = selectedType,
                onSelected = { 
                    selectedType = it
                    if (it == "LEAGUE" && homeAndAway) {
                        calendarGenerationMode = "ROUNDS"
                    } else if (it == "CUP") {
                        calendarGenerationMode = "ROUNDS"
                    }
                }
            )

            if (selectedType == "CUP") {
                OptionRow(
                    label = "Formato Torneo",
                    options = listOf(
                        "SINGLE_ELIMINATION" to "Eliminazione Diretta",
                        "GROUPS_THEN_SINGLE_ELIMINATION" to "Gironi + Eliminazione"
                    ),
                    selected = tournamentFormat ?: "GROUPS_THEN_SINGLE_ELIMINATION",
                    onSelected = { tournamentFormat = it }
                )
            }

            CompetitionTemplatePicker(
                templates = s.competitionTemplates,
                selectedTemplateSportId = selectedTemplateSportId,
                onSelected = { template ->
                    if (template == null) {
                        selectedTemplateSportId = null
                        selectedSportId = null
                    } else {
                        applyTemplate(template)
                    }
                }
            )

            OptionRow(
                label = "Classifiche",
                options = listOf("PLAYER" to "Giocatori", "TEAM" to "Squadre", "BOTH" to "Entrambe"),
                selected = selectedRankingMode,
                onSelected = { selectedRankingMode = it }
            )

            OptionRow(
                label = "Tipo partita",
                options = listOf("SINGLE" to "Singolo", "DOUBLE" to "Doppio", "TEAM" to "Squadra"),
                selected = selectedMatchType,
                onSelected = { selectedMatchType = it }
            )

            ToggleLine("Partecipo anche io", joinCreator) { joinCreator = it }
                }

                1 -> {
        CreationSectionCard("Formato competizione") {
            OptionRow(
                label = "Ranking",
                options = listOf("POINTS" to "Punti", "ELO" to "ELO", "WIN_RATE" to "% vitt."),
                selected = selectedCompRankingType,
                onSelected = { selectedCompRankingType = it }
            )

            OptionRow(
                label = "Formato partita",
                options = listOf("POINTS" to "A punti", "SETS" to "A set", "GOALS" to "Gol"),
                selected = selectedMatchFormat,
                onSelected = { selectedMatchFormat = it }
            )

            OptionRow(
                label = "Creazione partite",
                options = listOf("FREE" to "Libera", "SCHEDULED" to "Calendario"),
                selected = matchCreationMode,
                onSelected = {
                    matchCreationMode = it
                    if (it == "SCHEDULED" && selectedType == "LEAGUE" && homeAndAway) {
                        calendarGenerationMode = "ROUNDS"
                    }
                }
            )

            if (matchCreationMode == "SCHEDULED") {
                OptionRow(
                    label = "Modalità calendario",
                    options = listOf("SEQUENTIAL" to "Sequenziale", "ROUNDS" to "A giornate"),
                    selected = if ((selectedType == "LEAGUE" && homeAndAway) || selectedType == "CUP") "ROUNDS" else calendarGenerationMode,
                    onSelected = { calendarGenerationMode = it }
                )
            }
        }

        CreationSectionCard("Punti classifica") {
            ToggleLine("Consenti pareggio", allowDraw) { allowDraw = it }
            ToggleLine("Vittoria con scarto 2", winByTwo) { winByTwo = it }
            if (selectedCompRankingType == "POINTS") {
                NumberStepperLine("Punti vittoria", winPoints, 0..100) { winPoints = it }
                if (allowDraw) {
                    NumberStepperLine("Punti pareggio", drawPoints, 0..100) { drawPoints = it }
                }
                NumberStepperLine("Punti sconfitta", lossPoints, 0..100) { lossPoints = it }
            } else {
                Text(
                    "I punti classifica vengono usati solo con ranking a punti.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (selectedMatchFormat == "POINTS") {
            CreationSectionCard("Target score") {
                ToggleLine("Usa target score", useTargetScore) { useTargetScore = it }
                if (useTargetScore) {
                    NumberStepperLine("Target score", targetScore, 1..100) { targetScore = it }
                }
            }
        }

        CreationSectionCard("Regole partita") {
            ToggleLine("Cappotto abilitato", cappottoEnabled) { cappottoEnabled = it }
            if (cappottoEnabled) {
                NumberStepperLine("Bonus cappotto", cappottoBonusPoints, 0..100) { cappottoBonusPoints = it }
            }
            ToggleLine("Andata e ritorno", homeAndAway) { 
                homeAndAway = it
                if (it && selectedType == "LEAGUE") {
                    calendarGenerationMode = "ROUNDS"
                }
            }
        }
                }

                2 -> {
        CreationSectionCard("Date") {
            var showStartPicker by remember { mutableStateOf(false) }
            var showEndPicker by remember { mutableStateOf(false) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { },
                    label = { Text("Data inizio") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showStartPicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    },
                    modifier = Modifier.weight(1f).clickable { showStartPicker = true },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { },
                    label = { Text("Data fine") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showEndPicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        }
                    },
                    modifier = Modifier.weight(1f).clickable { showEndPicker = true },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
            }

            if (showStartPicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showStartPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                startDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            }
                            showStartPicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartPicker = false }) { Text("Annulla") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (showEndPicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = LocalDate.parse(endDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showEndPicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                endDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            }
                            showEndPicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndPicker = false }) { Text("Annulla") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (!validDates) {
                Text(
                    "La data fine non può essere precedente alla data inizio.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        CreationSectionCard("Copia dati") {
            ToggleLine("Copia partecipanti", copyParticipants) {
                copyParticipants = it
                if (!it) copyTeams = false
            }
            ToggleLine("Copia squadre", copyTeams) {
                copyTeams = it
                if (it) copyParticipants = true
            }

            if (shouldCopy) {
                if (s.competitions.isEmpty()) {
                    Text(
                        "Nessuna competizione disponibile da cui copiare.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    SourceCompetitionPicker(
                        competitions = s.competitions,
                        selectedCompetitionId = copyFromCompetitionId,
                        onSelected = { copyFromCompetitionId = it }
                    )
                }
            }
        }
                }

                3 -> CompetitionReview(
                    name = name,
                    type = selectedType,
                    sport = s.sports.firstOrNull { it.id == selectedSportId }?.name
                        ?: s.competitionTemplates.firstOrNull { it.sportId == selectedSportId }?.sportName
                        ?: "Sport",
                    rankingMode = selectedRankingMode,
                    matchType = selectedMatchType,
                    rankingType = selectedCompRankingType,
                    matchFormat = selectedMatchFormat,
                    startDate = startDate,
                    endDate = endDate,
                    joinCreator = joinCreator,
                    copyParticipants = copyParticipants,
                    copyTeams = copyTeams,
                    tournamentFormat = if (selectedType == "CUP") tournamentFormat else null
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        CompetitionCreationNavigation(
            currentStep = currentStep,
            canContinue = canContinue,
            canCreate = canCreate,
            loading = s.loading,
            onBack = { currentStep = (currentStep - 1).coerceAtLeast(0) },
            onNext = { currentStep = (currentStep + 1).coerceAtMost(3) },
            onCreate = {
                val sportId = selectedSportId ?: return@CompetitionCreationNavigation
                val finalCalendarMode = if (selectedType == "LEAGUE" && homeAndAway) "ROUNDS" else calendarGenerationMode

                vm.createCompetition(
                    league,
                    season,
                    CreateCompetitionRequest(
                        name = name.trim(),
                        type = selectedType,
                        startDate = DateUtils.toIsoDate(startDate),
                        endDate = DateUtils.toIsoDate(endDate),
                        copyFromCompetitionId = if (shouldCopy) copyFromCompetitionId else null,
                        copyParticipants = shouldCopy && copyParticipants,
                        copyTeams = shouldCopy && copyTeams,
                        rankingMode = selectedRankingMode,
                        matchType = selectedMatchType,
                        competitionRankingType = selectedCompRankingType,
                        sportId = sportId,
                        joinCreator = joinCreator,
                        targetScore = if (selectedMatchFormat == "POINTS" && useTargetScore) targetScore else null,
                        useTargetScore = selectedMatchFormat == "POINTS" && useTargetScore,
                        allowDraw = allowDraw,
                        winPoints = if (selectedCompRankingType == "POINTS") winPoints else 0,
                        drawPoints = if (selectedCompRankingType == "POINTS" && allowDraw) drawPoints else 0,
                        lossPoints = if (selectedCompRankingType == "POINTS") lossPoints else 0,
                        cappottoEnabled = cappottoEnabled,
                        cappottoBonusPoints = if (cappottoEnabled) cappottoBonusPoints else 0,
                        matchFormat = selectedMatchFormat,
                        winByTwo = winByTwo,
                        matchCreationMode = matchCreationMode,
                        calendarGenerationMode = finalCalendarMode,
                        homeAndAway = homeAndAway,
                        tournamentFormat = if (selectedType == "CUP") tournamentFormat else null
                    )
                )
            }
        )
    }
}

@Composable
private fun CompetitionCreationProgress(currentStep: Int) {
    val steps = listOf("Dettagli", "Regole", "Date", "Riepilogo")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Passaggio ${currentStep + 1} di ${steps.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        steps[currentStep],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "${((currentStep + 1) * 100) / steps.size}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { (currentStep + 1) / steps.size.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEachIndexed { index, title ->
                    Text(
                        title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index <= currentStep) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (index == currentStep) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun CompetitionCreationNavigation(
    currentStep: Int,
    canContinue: Boolean,
    canCreate: Boolean,
    loading: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onCreate: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.height(50.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Indietro")
                }
            }

            Button(
                onClick = if (currentStep == 3) onCreate else onNext,
                enabled = if (currentStep == 3) canCreate else canContinue,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                if (loading && currentStep == 3) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else if (currentStep == 3) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Crea campionato")
                } else {
                    Text("Continua")
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(19.dp))
                }
            }
        }
    }
}

@Composable
private fun CompetitionReview(
    name: String,
    type: String,
    sport: String,
    rankingMode: String,
    matchType: String,
    rankingType: String,
    matchFormat: String,
    startDate: String,
    endDate: String,
    joinCreator: Boolean,
    copyParticipants: Boolean,
    copyTeams: Boolean,
    tournamentFormat: String? = null
) {
    CreationSectionCard("Riepilogo campionato") {
        ReviewLine("Nome", name)
        ReviewLine("Sport", sport)
        ReviewLine("Tipo", type.creationLabel())
        if (type == "CUP" && tournamentFormat != null) {
            ReviewLine("Formato Torneo", tournamentFormat.creationLabel())
        }
        ReviewLine("Classifiche", rankingMode.creationLabel())
        ReviewLine("Tipo partita", matchType.creationLabel())
        ReviewLine("Ranking", rankingType.creationLabel())
        ReviewLine("Formato", matchFormat.creationLabel())
        ReviewLine("Periodo", "$startDate - $endDate")
    }

    CreationSectionCard("Partecipazione e copia") {
        ReviewLine("Partecipo anche io", if (joinCreator) "Sì" else "No")
        ReviewLine("Copia partecipanti", if (copyParticipants) "Sì" else "No")
        ReviewLine("Copia squadre", if (copyTeams) "Sì" else "No")
    }

    Text(
        "Controlla i dati prima di creare il campionato. Puoi tornare indietro senza perdere le impostazioni.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ReviewLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

private fun String.creationLabel(): String = when (this) {
    "LEAGUE" -> "Campionato"
    "CUP" -> "Torneo"
    "PLAYER" -> "Giocatori"
    "TEAM" -> "Squadre"
    "BOTH" -> "Entrambe"
    "SINGLE" -> "Singolo"
    "DOUBLE" -> "Doppio"
    "POINTS" -> "Punti"
    "ELO" -> "ELO"
    "WIN_RATE" -> "Percentuale vittorie"
    "SETS" -> "Set"
    "GOALS" -> "Gol"
    "SINGLE_ELIMINATION" -> "Eliminazione Diretta"
    "GROUPS_THEN_SINGLE_ELIMINATION" -> "Gironi + Eliminazione"
    else -> this
}

@Composable
private fun CreationSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, title) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    label = { Text(title) }
                )
            }
        }
    }
}

@Composable
private fun CompetitionTemplatePicker(
    templates: List<CompetitionTemplateResponse>,
    selectedTemplateSportId: Long?,
    onSelected: (CompetitionTemplateResponse?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTemplate = templates.firstOrNull { it.sportId == selectedTemplateSportId }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Template sport", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    selectedTemplate?.sportName ?: "Nessun template",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Nessun template") },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    }
                )
                templates.forEach { template ->
                    DropdownMenuItem(
                        text = { Text(template.sportName) },
                        onClick = {
                            onSelected(template)
                            expanded = false
                        }
                    )
                }
            }
        }
        Text(
            "Il template compila automaticamente formato e regole. Puoi lasciare questa scelta vuota.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ToggleLine(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NumberStepperLine(label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            onClick = { onValueChange((value - 1).coerceAtLeast(range.first)) },
            enabled = value > range.first,
            modifier = Modifier.size(40.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("-")
        }
        Text(
            value.toString(),
            modifier = Modifier.width(44.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        OutlinedButton(
            onClick = { onValueChange((value + 1).coerceAtMost(range.last)) },
            enabled = value < range.last,
            modifier = Modifier.size(40.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("+")
        }
    }
}

@Composable
private fun SourceCompetitionPicker(
    competitions: List<CompetitionResponse>,
    selectedCompetitionId: Long?,
    onSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCompetition = competitions.firstOrNull { it.id == selectedCompetitionId }

    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                selectedCompetition?.name ?: "Seleziona competizione",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Seleziona competizione") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            competitions.forEach { competition ->
                DropdownMenuItem(
                    text = { Text(competition.name) },
                    onClick = {
                        onSelected(competition.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
