package com.biliardino.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.biliardino.model.*
import com.biliardino.ui.components.MatchList
import com.biliardino.ui.components.StatRow
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun TeamDetailScreen(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse, team: TeamResponse, s: UiState, vm: AppViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isTeamType = competition.matchType == "TEAM"
    val tabs = if (isTeamType) listOf("Statistiche", "Partite", "Compare", "Membri") else listOf("Statistiche", "Partite", "Compare")

    LaunchedEffect(team.id) {
        vm.loadTeamMembers(team.id)
    }

    Column(Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(team.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                val subtitle = when (competition.matchType) {
                    "TEAM" -> null
                    "SINGLE" -> team.playerAUsername
                    else -> if (team.playerAUsername != null && team.playerBUsername != null) {
                        "${team.playerAUsername} & ${team.playerBUsername}"
                    } else {
                        team.playerAUsername ?: team.playerBUsername
                    }
                }
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        Box(Modifier.weight(1f).padding(16.dp)) {
            when (selectedTab) {
                0 -> Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TeamStatsInfo(s.currentTeamStats, isTeamType)
                    if (s.currentTeamRatingHistory.isNotEmpty() && !isTeamType) {
                        Text("Andamento Rating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        RatingLineChart(s.currentTeamRatingHistory, Modifier.fillMaxWidth().height(200.dp))
                    }
                }
                1 -> {
                    val isAdmin = s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER"
                    MatchList(
                        matches = s.currentTeamMatches,
                        teams = s.seasonTeams,
                        isAdmin = isAdmin,
                        rankingType = competition.competitionRankingType,
                        calendarGenerationMode = competition.calendarGenerationMode,
                        competitionType = competition.type,
                        onDeleteMatch = { matchId -> vm.deleteMatch(competition.id, matchId) },
                        onUpdateResult = { matchId, sA, sB -> vm.updateMatchResult(competition.id, matchId, sA, sB) }
                    )
                }
                2 -> TeamCompareView(competition, team, s.seasonTeams, s.currentTeamHeadToHead, vm)
                3 -> TeamMembersView(team, s.teamMembers, s.currentUser?.userId, s.currentUserRoleInLeague)
            }
        }
    }
}

@Composable
fun TeamMembersView(team: TeamResponse, members: List<TeamMemberResponse>, currentUserId: Long?, currentUserRoleInLeague: String?) {
    val clipboardManager = LocalClipboardManager.current
    val isMember = members.any { it.userId == currentUserId }
    val isAdminOrOwner = currentUserRoleInLeague == "ADMIN" || currentUserRoleInLeague == "OWNER"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if ((isMember || isAdminOrOwner) && team.inviteCode != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Codice Invito Squadra", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = team.inviteCode,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(team.inviteCode)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copia")
                        }
                    }
                    Text(
                        "Condividi questo codice con i tuoi compagni per farli entrare nella squadra.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Text("Componenti della squadra", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (members.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            members.forEach { member ->
                ListItem(
                    headlineContent = { Text(member.username ?: "Utente ${member.userId ?: 0}", fontWeight = FontWeight.Bold) },
                    trailingContent = {
                        val role = member.role ?: "PLAYER"
                        Surface(
                            color = if (role == "OWNER") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ) {
                            Text(
                                text = role,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun TeamStatsInfo(stats: TeamStatsResponse?, isTeamType: Boolean = false) {
    if (stats == null) return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Generali", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                if (!isTeamType) {
                    StatRow("Rating", "${stats.rating}")
                }
                StatRow("Partite Giocate", "${stats.matchesPlayed}")
                if (isTeamType) {
                    StatRow("Vittorie", "${stats.wins}")
                    StatRow("Pareggi", "${stats.draws}")
                    StatRow("Sconfitte", "${stats.losses}")
                    StatRow("Punti", "${stats.points}")
                } else {
                    StatRow("Vittorie / Sconfitte", "${stats.wins} / ${stats.losses}")
                }
                StatRow("Win Rate", "${"%.1f".format(stats.winPercentage)}%")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(if (isTeamType) "Gol" else "Gol e Cappotti", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                StatRow("Gol Fatti", "${stats.goalsFor}")
                StatRow("Gol Subiti", "${stats.goalsAgainst}")
                StatRow("Diff. Gol", "${stats.goalDifference}")
                if (!isTeamType) {
                    StatRow("Cappotti Dati", "${stats.cappottiGiven}")
                    StatRow("Cappotti Subiti", "${stats.cappottiReceived}")
                }
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
fun TeamCompareView(competition: CompetitionResponse, teamA: TeamResponse, allTeams: List<TeamResponse>, h2h: HeadToHeadResponse?, vm: AppViewModel) {
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
                            vm.loadHeadToHead(competition.id, teamA.id, team.id)
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
