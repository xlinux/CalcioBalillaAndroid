package com.biliardino.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biliardino.model.MatchResponse
import com.biliardino.model.TeamResponse
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
    onUpdateResult: ((Long, Int, Int) -> Unit)? = null
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
                    } ?: "Data Sconosciuta"
                }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp)) {
        groupedMatches.forEach { (header, matchesInGroup) ->
            stickyHeader {
                Text(
                    text = header,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(matchesInGroup) { match ->
                MatchItem(match, teams, isAdmin, rankingType, onDeleteMatch, onUpdateResult)
                Spacer(Modifier.height(12.dp))
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
    onUpdateResult: ((Long, Int, Int) -> Unit)? = null
) {
    val teamA = match.teamAId?.let { id -> teams.find { it.id == id } }
    val teamB = match.teamBId?.let { id -> teams.find { it.id == id } }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }

    val playerA1 = teamA?.playerAUsername
    val playerA2 = teamA?.playerBUsername
    val playerB1 = teamB?.playerAUsername
    val playerB2 = teamB?.playerBUsername

    val isPlayed = match.scoreA != null && match.scoreB != null
    val isElo = rankingType == "ELO"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isPlayed && onUpdateResult != null) { showResultDialog = true },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            if (isAdmin && onDeleteMatch != null) {
                Box(Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Elimina Partita",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team A
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = match.teamAName ?: "TBD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    playerA1?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                    playerA2?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                    
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = match.scoreA?.toString() ?: "-",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isElo) {
                        match.teamARatingAfter?.let { rating ->
                            Text(
                                text = "$rating pt",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // VS and Info
                Column(
                    Modifier.weight(0.6f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "VS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isElo) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            match.teamARatingDelta?.let { delta ->
                                Text(
                                    text = "${if (delta >= 0) "+" else ""}$delta",
                                    color = if (delta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            match.teamBRatingDelta?.let { delta ->
                                Text(
                                    text = "${if (delta >= 0) "+" else ""}$delta",
                                    color = if (delta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (match.cappotto == true) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                "CAPPOTTO",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                // Team B
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = match.teamBName ?: "TBD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    playerB1?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                    playerB2?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = match.scoreB?.toString() ?: "-",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isElo) {
                        match.teamBRatingAfter?.let { rating ->
                            Text(
                                text = "$rating pt",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                TextButton(
                    onClick = {
                        onDeleteMatch(match.id)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ELIMINA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("ANNULLA")
                }
            }
        )
    }

    if (showResultDialog && onUpdateResult != null) {
        MatchResultDialog(
            match = match,
            onDismiss = { showResultDialog = false },
            onConfirm = { sA, sB ->
                onUpdateResult(match.id, sA, sB)
                showResultDialog = false
            }
        )
    }
}

@Composable
fun MatchResultDialog(
    match: MatchResponse,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var scoreA by remember { mutableIntStateOf(0) }
    var scoreB by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inserisci Risultato", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${match.teamAName} vs ${match.teamBName}", style = MaterialTheme.typography.bodyMedium)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScorePicker(scoreA) { scoreA = it }
                    Text("-", style = MaterialTheme.typography.headlineLarge)
                    ScorePicker(scoreB) { scoreB = it }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(scoreA, scoreB) }) {
                Text("Salva")
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
private fun ScorePicker(value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { onValueChange(value + 1) }) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = { if (value > 0) onValueChange(value - 1) }) {
            Text("-", style = MaterialTheme.typography.titleLarge)
        }
    }
}
