package com.biliardino.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biliardino.model.CompetitionResponse
import com.biliardino.model.CompetitionTemplateResponse
import com.biliardino.model.CreateCompetitionRequest
import com.biliardino.model.LeagueResponse
import com.biliardino.model.SeasonResponse
import com.biliardino.util.DateUtils
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun CreateCompetitionScreen(
    league: LeagueResponse,
    season: SeasonResponse,
    s: UiState,
    vm: AppViewModel
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("LEAGUE") }
    var selectedRankingMode by remember { mutableStateOf("BOTH") }
    var selectedMatchType by remember { mutableStateOf("SINGLE") }
    var selectedCompRankingType by remember { mutableStateOf("POINTS") }
    var selectedMatchFormat by remember { mutableStateOf("POINTS") }
    var winByTwo by remember { mutableStateOf(false) }
    var matchCreationMode by remember { mutableStateOf("FREE") }
    var selectedSportId by remember { mutableStateOf<Long?>(null) }
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
    }

    LaunchedEffect(Unit) {
        vm.loadCompetitionTemplates()
        vm.loadSports()
    }

    LaunchedEffect(s.competitionTemplates) {
        if (selectedSportId == null && s.competitionTemplates.isNotEmpty()) {
            applyTemplate(s.competitionTemplates.first())
        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CreationSectionCard("Dettagli") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome campionato") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                singleLine = true
            )

            OptionRow(
                label = "Tipo",
                options = listOf("LEAGUE" to "Campionato", "CUP" to "Torneo"),
                selected = selectedType,
                onSelected = { selectedType = it }
            )

            SportTemplatePicker(
                templates = s.competitionTemplates,
                selectedSportId = selectedSportId,
                onSelected = { template -> applyTemplate(template) }
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
                onSelected = { matchCreationMode = it }
            )

            ToggleLine("Partecipo anche io", joinCreator) { joinCreator = it }
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
            ToggleLine("Andata e ritorno", homeAndAway) { homeAndAway = it }
        }

        CreationSectionCard("Date") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Data inizio") },
                    placeholder = { Text("GG/MM/AAAA") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("Data fine") },
                    placeholder = { Text("GG/MM/AAAA") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )
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

        Button(
            onClick = {
                val sportId = selectedSportId ?: return@Button
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
                        homeAndAway = homeAndAway
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = canCreate,
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Crea campionato", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(24.dp))
    }
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
private fun SportTemplatePicker(
    templates: List<CompetitionTemplateResponse>,
    selectedSportId: Long?,
    onSelected: (CompetitionTemplateResponse) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTemplate = templates.firstOrNull { it.sportId == selectedSportId }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Sport", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        if (templates.isEmpty()) {
            Text(
                "Nessun template sport disponibile",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        selectedTemplate?.sportName ?: "Seleziona sport",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
        }
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
