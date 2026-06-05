package com.biliardino.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biliardino.model.MatchResponse
import com.biliardino.model.TeamResponse
import com.biliardino.util.DateUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MatchList(
    matches: List<MatchResponse>,
    teams: List<TeamResponse>,
    isAdmin: Boolean = false,
    rankingType: String? = "ELO",
    calendarGenerationMode: String? = "SEQUENTIAL",
    competitionType: String? = "LEAGUE",
    onDeleteMatch: ((Long) -> Unit)? = null,
    onUpdateResult: ((Long, Int, Int) -> Unit)? = null,
    onEditResult: ((Long, Int, Int) -> Unit)? = null,
    isCompetitionActive: Boolean = true
) {
    val isLeague = competitionType == "LEAGUE"
    val isRounds = calendarGenerationMode == "ROUNDS" || isLeague

    val groupedMatches = remember(matches, isLeague, isRounds) {
        if (isRounds) {
            matches.sortedWith(
                compareBy<MatchResponse> { it.roundNumber ?: Int.MAX_VALUE }
                    .thenBy { it.playedAt ?: "" }
                    .thenBy { it.id }
            ).groupBy { match ->
                if (isLeague) {
                    match.roundNumber?.let { "Giornata $it" } ?: "Altre partite"
                } else {
                    match.roundNumber?.let { round ->
                        when (round) {
                            1 -> "Finale"
                            2 -> "Semifinali"
                            4 -> "Quarti"
                            8 -> "Ottavi"
                            16 -> "Sedicesimi"
                            else -> "Turno $round"
                        }
                    } ?: "Altre partite"
                }
            }
        } else {
            matches.sortedByDescending { it.playedAt ?: "" }
                .groupBy { match ->
                    match.playedAt?.let {
                        try {
                            val dt = LocalDateTime.parse(it.substring(0, 19))
                            val date = dt.toLocalDate()
                            val today = LocalDate.now()
                            val yesterday = today.minusDays(1)
                            val dateFormatted = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            
                            when (date) {
                                today -> "OGGI ($dateFormatted)"
                                yesterday -> "IERI ($dateFormatted)"
                                else -> dateFormatted
                            }
                        } catch (e: Exception) {
                            "Altre"
                        }
                    } ?: "Da giocare"
                }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        groupedMatches.forEach { (header, matchesInGroup) ->
            stickyHeader {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    Text(
                        text = header.uppercase(),
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
            }
            items(matchesInGroup) { match ->
                MatchItem(
                    match = match,
                    teams = teams,
                    isAdmin = isAdmin,
                    rankingType = rankingType,
                    onDeleteMatch = onDeleteMatch,
                    onUpdateResult = onUpdateResult,
                    onEditResult = onEditResult,
                    competitionType = competitionType,
                    isCompetitionActive = isCompetitionActive
                )
            }
        }
    }
}

@Composable
fun MatchItem(
    match: MatchResponse,
    teams: List<TeamResponse>,
    isAdmin: Boolean = false,
    rankingType: String? = "ELO",
    onDeleteMatch: ((Long) -> Unit)? = null,
    onUpdateResult: ((Long, Int, Int) -> Unit)? = null,
    onEditResult: ((Long, Int, Int) -> Unit)? = null,
    competitionType: String? = "LEAGUE",
    isCompetitionActive: Boolean = true
) {
    val teamA = match.teamAId?.let { id -> teams.find { it.id == id } }
    val teamB = match.teamBId?.let { id -> teams.find { it.id == id } }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }

    val isPlayed = match.scoreA != null && match.scoreB != null
    val isElo = rankingType == "ELO"
    val isLeague = competitionType == "LEAGUE"

    val canUpdate = !isPlayed && onUpdateResult != null
    val canEdit = isPlayed && isLeague && isCompetitionActive && onEditResult != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canUpdate || canEdit) { showResultDialog = true },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayed) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlayed) 2.dp else 0.dp),
        border = if (!isPlayed) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header: Date or Admin actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isPlayed && !match.playedAt.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = DateUtils.formatDate(match.playedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (!isPlayed) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = CircleShape
                    ) {
                        Text(
                            "DA GIOCARE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                if (isAdmin && onDeleteMatch != null) {
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Teams and Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Team A
                TeamColumn(
                    name = match.teamAName ?: "TBD",
                    score = match.scoreA,
                    ratingDelta = if (isElo) match.teamARatingDelta else null,
                    isWinner = isPlayed && (match.scoreA ?: 0) > (match.scoreB ?: 0),
                    modifier = Modifier.weight(1f)
                )

                // VS
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (match.cappotto == true) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(
                                "CAPPOTTO",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 8.sp
                            )
                        }
                    } else {
                        Text(
                            "VS",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Team B
                TeamColumn(
                    name = match.teamBName ?: "TBD",
                    score = match.scoreB,
                    ratingDelta = if (isElo) match.teamBRatingDelta else null,
                    isWinner = isPlayed && (match.scoreB ?: 0) > (match.scoreA ?: 0),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
            
            if (canEdit) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Modifica Risultato", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && onDeleteMatch != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminare Partita?") },
            text = { Text("Sei sicuro di voler eliminare questa partita? Il punteggio e il rating verranno ricalcolati.") },
            confirmButton = {
                TextButton(onClick = { onDeleteMatch(match.id); showDeleteConfirm = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("ELIMINA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("ANNULLA") }
            }
        )
    }

    if (showResultDialog && (onUpdateResult != null || onEditResult != null)) {
        MatchResultDialog(
            match = match,
            isEdit = isPlayed,
            onDismiss = { showResultDialog = false },
            onConfirm = { sA, sB ->
                if (isPlayed) onEditResult?.invoke(match.id, sA, sB)
                else onUpdateResult?.invoke(match.id, sA, sB)
                showResultDialog = false
            }
        )
    }
}

@Composable
private fun TeamColumn(
    name: String,
    score: Int?,
    ratingDelta: Int?,
    isWinner: Boolean,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Column(modifier = modifier, horizontalAlignment = if (textAlign == TextAlign.Start) Alignment.Start else Alignment.End) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isWinner) FontWeight.Black else FontWeight.Bold,
            color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign
        )
        
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = score?.toString() ?: "-",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            
            ratingDelta?.let { delta ->
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${if (delta >= 0) "+" else ""}$delta",
                    color = if (delta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
fun MatchResultDialog(
    match: MatchResponse,
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var scoreA by remember { mutableIntStateOf(match.scoreA ?: 0) }
    var scoreB by remember { mutableIntStateOf(match.scoreB ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (isEdit) "Modifica Risultato" else "Inserisci Risultato", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black 
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "${match.teamAName} vs ${match.teamBName}",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScorePicker(scoreA) { scoreA = it }
                    Text(
                        "-", 
                        style = MaterialTheme.typography.displayLarge, 
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    ScorePicker(scoreB) { scoreB = it }
                }
                
                if (isEdit) {
                    Text(
                        "Attenzione: la modifica del risultato aggiornerà automaticamente la classifica e i rating ELO dei giocatori.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(scoreA, scoreB) },
                shape = MaterialTheme.shapes.large,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("CONFERMA", fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ANNULLA")
            }
        },
        shape = MaterialTheme.shapes.extraLarge
    )
}

@Composable
private fun ScorePicker(value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalIconButton(
            onClick = { onValueChange(value + 1) },
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        
        Surface(
            modifier = Modifier.width(64.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = value.toString(),
                modifier = Modifier.padding(vertical = 12.dp),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
        
        FilledTonalIconButton(
            onClick = { if (value > 0) onValueChange(value - 1) },
            modifier = Modifier.size(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("-", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
