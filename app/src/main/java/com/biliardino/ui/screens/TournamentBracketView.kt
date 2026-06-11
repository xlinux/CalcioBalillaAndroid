package com.biliardino.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biliardino.model.MatchResponse
import com.biliardino.util.DateUtils
import kotlin.math.log2
import kotlin.math.pow

private data class BracketRoundSection(
    val round: Int,
    val title: String,
    val matches: List<MatchResponse>
)

@Composable
fun TournamentBracketView(
    matches: List<MatchResponse>,
    onMatchClick: (MatchResponse) -> Unit
) {
    if (matches.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nessuna partita presente", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val rounds = remember(matches) { bracketRounds(matches) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(rounds, key = { _, round -> round.round }) { index, round ->
            BracketRoundCard(
                round = round,
                index = index,
                isFinal = index == rounds.lastIndex,
                onMatchClick = onMatchClick
            )

            if (index < rounds.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = roundTint(index).copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BracketRoundCard(
    round: BracketRoundSection,
    index: Int,
    isFinal: Boolean,
    onMatchClick: (MatchResponse) -> Unit
) {
    val tint = roundTint(index)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(tint)
            )

            Column(
                modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BracketRoundHeader(round, tint, isFinal)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    round.matches.forEach { match ->
                        BracketMatchCard(match, tint, onMatchClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun BracketRoundHeader(
    round: BracketRoundSection,
    tint: Color,
    isFinal: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    color = tint.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = if (isFinal) Icons.Default.EmojiEvents else Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.padding(7.dp),
                        tint = tint
                    )
                }

                Text(
                    text = round.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                BracketBadge(
                    text = if (round.round == Int.MAX_VALUE) "TBD" else "Turno ${round.round}",
                    tint = tint
                )
                Text(
                    text = "${round.matches.size} ${if (round.matches.size == 1) "partita" else "partite"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun BracketBadge(text: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BracketMatchCard(
    match: MatchResponse,
    tint: Color,
    onMatchClick: (MatchResponse) -> Unit
) {
    val hasResult = match.scoreA != null && match.scoreB != null
    val teamsDefined = hasTeam(match.teamAId, match.teamAName) && hasTeam(match.teamBId, match.teamBName)
    val canInputResult = teamsDefined && !hasResult && match.resultInsertable
    val disabledMessage = when {
        teamsDefined && !hasResult && !match.resultInsertable -> "Completa prima il turno precedente"
        else -> null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (canInputResult || hasAnyTeam(match)) 1f else 0.68f)
            .clickable(enabled = canInputResult) { onMatchClick(match) },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = when {
                canInputResult -> tint.copy(alpha = 0.38f)
                disabledMessage != null -> Color(0xFFFF9800).copy(alpha = 0.32f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
            }
        ),
        tonalElevation = if (canInputResult) 1.dp else 0.dp
    ) {
        Column {
            if (hasResult && match.playedAt != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = DateUtils.formatDate(match.playedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
            }

            TeamRow(
                name = displayName(match.teamAName),
                score = match.scoreA,
                isWinner = isWinner(match.scoreA, match.scoreB),
                tint = tint
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))

            TeamRow(
                name = displayName(match.teamBName),
                score = match.scoreB,
                isWinner = isWinner(match.scoreB, match.scoreA),
                tint = tint
            )

            if (disabledMessage != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                Text(
                    text = disabledMessage,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TeamRow(name: String, score: Int?, isWinner: Boolean, tint: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = if (name == "Da definire") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = score?.toString() ?: "-",
            modifier = Modifier.width(28.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isWinner) tint else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

private fun bracketRounds(matches: List<MatchResponse>): List<BracketRoundSection> {
    val bracketMatches = matches.filter { it.bracketRound != null }
    if (bracketMatches.isEmpty()) return inferredBracketRounds(matches)

    val groupedByRound = bracketMatches.groupBy { it.bracketRound ?: 0 }
    val maxRound = groupedByRound.keys.maxOrNull() ?: 0
    val rounds = groupedByRound.keys.sorted().map { round ->
        BracketRoundSection(
            round = round,
            title = bracketRoundTitle(round, maxRound),
            matches = sortedByBracketStructure(groupedByRound[round].orEmpty())
        )
    }.toMutableList()

    val matchesWithoutRound = matches.filter { it.bracketRound == null }
    if (matchesWithoutRound.isNotEmpty()) {
        rounds.add(
            BracketRoundSection(
                round = Int.MAX_VALUE,
                title = "Da definire",
                matches = sortedByBracketStructure(matchesWithoutRound)
            )
        )
    }

    return rounds
}

private fun inferredBracketRounds(matches: List<MatchResponse>): List<BracketRoundSection> {
    val sortedMatches = sortedByBracketStructure(matches)
    val inferredTeams = sortedMatches.size + 1
    val validBracketSizes = setOf(4, 8, 16, 32)

    if (inferredTeams !in validBracketSizes) {
        return listOf(
            BracketRoundSection(
                round = 1,
                title = "Turno 1",
                matches = sortedMatches
            )
        )
    }

    val maxRound = log2(inferredTeams.toDouble()).toInt()
    var startIndex = 0

    return (1..maxRound).mapNotNull { round ->
        val roundSize = inferredTeams / 2.0.pow(round.toDouble()).toInt()
        if (roundSize <= 0 || startIndex >= sortedMatches.size) return@mapNotNull null
        val endIndex = minOf(startIndex + roundSize, sortedMatches.size)
        val roundMatches = sortedMatches.subList(startIndex, endIndex)
        startIndex = endIndex

        BracketRoundSection(
            round = round,
            title = bracketRoundTitle(round, maxRound),
            matches = sortedByBracketStructure(roundMatches)
        )
    }
}

private fun sortedByBracketStructure(matches: List<MatchResponse>): List<MatchResponse> {
    return matches.sortedWith(
        compareBy<MatchResponse> { it.bracketRound ?: Int.MAX_VALUE }
            .thenBy { it.bracketPosition ?: Int.MAX_VALUE }
            .thenBy { it.id }
    )
}

private fun bracketRoundTitle(round: Int, maxRound: Int): String {
    return when (maxRound - round) {
        0 -> "Finale"
        1 -> "Semifinali"
        2 -> "Quarti"
        3 -> "Ottavi"
        4 -> "Sedicesimi"
        else -> "Turno $round"
    }
}

private fun roundTint(index: Int): Color {
    return when (index) {
        0 -> Color(0xFF2563EB)
        1 -> Color(0xFF4F46E5)
        2 -> Color(0xFF7C3AED)
        else -> Color(0xFFF97316)
    }
}

private fun displayName(value: String?): String {
    val trimmed = value?.trim()
    return if (trimmed.isNullOrEmpty()) "Da definire" else trimmed
}

private fun hasTeam(id: Long?, name: String?): Boolean {
    return id != null || !name?.trim().isNullOrEmpty()
}

private fun hasAnyTeam(match: MatchResponse): Boolean {
    return hasTeam(match.teamAId, match.teamAName) || hasTeam(match.teamBId, match.teamBName)
}

private fun isWinner(score: Int?, otherScore: Int?): Boolean {
    if (score == null || otherScore == null) return false
    return score > otherScore
}
