package com.biliardino.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biliardino.model.*
import com.biliardino.ui.components.StatRow
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun TeamDetailScreen(league: LeagueResponse, season: SeasonResponse, team: TeamResponse, s: UiState, vm: AppViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Statistiche", "Compare")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        Box(Modifier.weight(1f).padding(16.dp)) {
            when (selectedTab) {
                0 -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TeamStatsInfo(s.currentTeamStats)
                    if (s.currentTeamRatingHistory.isNotEmpty()) {
                        Text("Andamento Rating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        RatingLineChart(s.currentTeamRatingHistory, Modifier.fillMaxWidth().height(200.dp))
                    }
                }
                1 -> TeamCompareView(season, team, s.seasonTeams, s.currentTeamHeadToHead, vm)
            }
        }
    }
}

@Composable
fun TeamStatsInfo(stats: TeamStatsResponse?) {
    if (stats == null) return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Generali", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                StatRow("Rating", "${stats.rating}")
                StatRow("Partite Giocate", "${stats.matchesPlayed}")
                StatRow("Vittorie / Sconfitte", "${stats.wins} / ${stats.losses}")
                StatRow("Win Rate", "${"%.1f".format(stats.winPercentage)}%")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Gol e Cappotti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                StatRow("Gol Fatti", "${stats.goalsFor}")
                StatRow("Gol Subiti", "${stats.goalsAgainst}")
                StatRow("Diff. Gol", "${stats.goalDifference}")
                StatRow("Cappotti Dati", "${stats.cappottiGiven}")
                StatRow("Cappotti Subiti", "${stats.cappottiReceived}")
            }
        }
    }
}

@Composable
fun RatingLineChart(history: List<RatingHistoryResponse>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.padding(8.dp)) {
        val ratings = history.map { it.ratingAfter?.toFloat() ?: 0f }
        if (ratings.isEmpty()) return@Canvas

        val minRating = ratings.minOrNull() ?: 0f
        val maxRating = ratings.maxOrNull() ?: 1000f
        val range = (maxRating - minRating).coerceAtLeast(1f)

        val width = size.width
        val height = size.height
        val stepX = width / (ratings.size - 1).coerceAtLeast(1)

        val points = ratings.mapIndexed { index, rating ->
            val x = index * stepX
            val y = height - ((rating - minRating) / range * height)
            Offset(x, y)
        }

        // Draw path
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw points
        points.forEach { point ->
            drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = point)
        }
    }
}

@Composable
fun TeamCompareView(season: SeasonResponse, teamA: TeamResponse, allTeams: List<TeamResponse>, h2h: HeadToHeadResponse?, vm: AppViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTeamB by remember { mutableStateOf<TeamResponse?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Confronta con un'altra squadra", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedTeamB?.name ?: "Seleziona Squadra")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                allTeams.filter { it.id != teamA.id && !haveSharedPlayers(it, teamA) }.forEach { team ->
                    DropdownMenuItem(
                        text = { Text(team.name) },
                        onClick = {
                            selectedTeamB = team
                            expanded = false
                            vm.loadHeadToHead(season.id, teamA.id, team.id)
                        }
                    )
                }
            }
        }

        if (h2h != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Scontri Diretti", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(h2h.teamAName, fontWeight = FontWeight.Bold)
                            Text("${h2h.teamAWins}", style = MaterialTheme.typography.headlineLarge)
                        }
                        Text("V", Modifier.align(Alignment.CenterVertically))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(h2h.teamBName, fontWeight = FontWeight.Bold)
                            Text("${h2h.teamBWins}", style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Totale Match: ${h2h.totalMatches}", style = MaterialTheme.typography.labelSmall)
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Distribuzione Vittorie", style = MaterialTheme.typography.labelMedium)
                    WinDistributionBar(h2h.teamAWins, h2h.teamBWins, Modifier.fillMaxWidth().height(24.dp).padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun WinDistributionBar(winsA: Int, winsB: Int, modifier: Modifier = Modifier) {
    val total = (winsA + winsB).coerceAtLeast(1)
    val ratioA = winsA.toFloat() / total
    val colorA = MaterialTheme.colorScheme.primary
    val colorB = MaterialTheme.colorScheme.error

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val split = width * ratioA

        drawRect(color = colorA, size = Size(split, height))
        drawRect(
            color = colorB,
            topLeft = Offset(split, 0f),
            size = Size(width - split, height)
        )
    }
}

private fun haveSharedPlayers(teamA: TeamResponse?, teamB: TeamResponse?): Boolean {
    if (teamA == null || teamB == null) return false
    val playersA = setOf(teamA.playerAId, teamA.playerBId)
    val playersB = setOf(teamB.playerAId, teamB.playerBId)
    return playersA.intersect(playersB).isNotEmpty()
}
