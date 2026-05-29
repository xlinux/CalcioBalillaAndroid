package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biliardino.model.CompetitionResponse
import com.biliardino.model.CreateCompetitionRequest
import com.biliardino.model.LeagueResponse
import com.biliardino.model.SeasonResponse
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun SeasonCompetitionsScreen(
    league: LeagueResponse,
    season: SeasonResponse,
    s: UiState,
    vm: AppViewModel
) {
    var competitionToJoin by remember { mutableStateOf<CompetitionResponse?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (s.competitions.isEmpty() && !s.loading) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(
                            Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "Nessuna Competizione",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Non ci sono competizioni configurate per questa stagione. Gli amministratori possono crearne di nuove usando il tasto +.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Competizioni Disponibili",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(s.competitions) { competition ->
                        CompetitionCard(
                            competition = competition,
                            onClick = {
                                if (competition.currentUserJoined) {
                                    vm.selectCompetition(league, season, competition)
                                } else {
                                    competitionToJoin = competition
                                }
                            }
                        )
                    }
                }
            }
        }

        if (s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER") {
            if (season.active != false) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crea Competizione")
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCompetitionDialog(
            competitions = s.competitions,
            onDismiss = { showCreateDialog = false },
            onConfirm = { request ->
                vm.createCompetition(league, season, request)
                showCreateDialog = false
            }
        )
    }

    if (competitionToJoin != null) {
        AlertDialog(
            onDismissRequest = { competitionToJoin = null },
            title = { Text("Partecipa alla Competizione", fontWeight = FontWeight.Bold) },
            text = { Text("Vuoi iscriverti alla competizione \"${competitionToJoin?.name}\" per iniziare a registrare le tue partite?") },
            confirmButton = {
                Button(
                    onClick = {
                        competitionToJoin?.let { vm.joinCompetition(league, season, it.id) }
                        competitionToJoin = null
                    },
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Partecipa Ora")
                }
            },
            dismissButton = {
                TextButton(onClick = { competitionToJoin = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
fun CreateCompetitionDialog(
    competitions: List<CompetitionResponse>,
    onDismiss: () -> Unit,
    onConfirm: (CreateCompetitionRequest) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val types = listOf("LEAGUE", "CUP")
    var selectedType by remember { mutableStateOf(types[0]) }
    val rankingModes = listOf(
        Triple("PLAYER", "Classifica giocatori", "Mostra solo la classifica dei giocatori"),
        Triple("TEAM", "Classifica squadre", "Mostra solo la classifica delle squadre"),
        Triple("BOTH", "Entrambe", "Mostra entrambe le classifiche")
    )
    var selectedRankingMode by remember { mutableStateOf("BOTH") }

    var copyParticipants by remember { mutableStateOf(false) }
    var copyTeams by remember { mutableStateOf(false) }
    var sourceCompetitionId by remember { mutableStateOf<Long?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova Competizione", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Competizione") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )
                
                Column {
                    Text("Tipologia di Evento", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    types.forEach { type ->
                        val isSelected = selectedType == type
                        Surface(
                            onClick = { selectedType = type },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                RadioButton(selected = isSelected, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (type == "LEAGUE") "Campionato" else "Torneo",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = if (type == "LEAGUE") "Partite tutti contro tutti con classifica" else "Scontri diretti a eliminazione",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Column {
                    Text("Classifiche", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    rankingModes.forEach { (mode, title, description) ->
                        val isSelected = selectedRankingMode == mode
                        Surface(
                            onClick = { selectedRankingMode = mode },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                RadioButton(selected = isSelected, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Column {
                    Text("Copia Dati (Opzionale)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = copyParticipants,
                            onCheckedChange = {
                                copyParticipants = it
                                if (!it) copyTeams = false
                            }
                        )
                        Text("Copia partecipanti", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = copyTeams,
                            onCheckedChange = {
                                copyTeams = it
                                if (it) copyParticipants = true
                            }
                        )
                        Text("Copia squadre", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (copyParticipants || copyTeams) {
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                val sourceName = competitions.find { it.id == sourceCompetitionId }?.name ?: "Seleziona competizione sorgente"
                                Text(sourceName)
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                competitions.forEach { comp ->
                                    DropdownMenuItem(
                                        text = { Text(comp.name) },
                                        onClick = {
                                            sourceCompetitionId = comp.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val isReady = name.isNotBlank() && (!copyParticipants || sourceCompetitionId != null)
            Button(
                onClick = {
                    onConfirm(
                        CreateCompetitionRequest(
                            name = name,
                            type = selectedType,
                            rankingMode = selectedRankingMode,
                            copyFromCompetitionId = sourceCompetitionId,
                            copyParticipants = copyParticipants,
                            copyTeams = copyTeams
                        )
                    )
                },
                enabled = isReady,
                shape = MaterialTheme.shapes.large
            ) {
                Text("Crea Competizione")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun CompetitionCard(competition: CompetitionResponse, onClick: () -> Unit) {
    val isLeague = competition.type == "LEAGUE"
    val isActive = competition.status == "ACTIVE" || competition.status == null

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.large,
                color = if (isLeague) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Icon(
                    imageVector = if (isLeague) Icons.Default.Groups else Icons.Default.MilitaryTech,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp).fillMaxSize(),
                    tint = if (isLeague) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(Modifier.width(20.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = competition.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val typeLabel = if (isLeague) "Campionato" else "Torneo"
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (isActive) "In corso" else "Concluso",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )
                }
            }

            if (competition.currentUserJoined) {
                Surface(
                    color = MaterialTheme.colorScheme.successContainer,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text(
                        text = "ISCRITTO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSuccessContainer
                        )
                    )
                }
            }
        }
    }
}

// Add successContainer to colorScheme extensions if not present, or use a hardcoded value
val ColorScheme.successContainer: Color get() = Color(0xFFC8E6C9)
val ColorScheme.onSuccessContainer: Color get() = Color(0xFF2E7D32)
