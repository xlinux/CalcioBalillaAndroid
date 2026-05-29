package com.biliardino.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biliardino.model.*
import com.biliardino.ui.Screen
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CompetitionStatisticsScreen(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse, s: UiState, vm: AppViewModel) {
    // Carichiamo i dati all'avvio se la lista è vuota
    LaunchedEffect(competition.id) {
        vm.loadRankings(competition.id, competition.rankingMode)
    }

    Column(Modifier.fillMaxSize()) {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Classifica", "Partite", "Squadre", "Giocatori")
        val showPlayerRanking = competition.rankingMode != "TEAM"
        val showTeamRanking = competition.rankingMode != "PLAYER"

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> Column {
                    var rankingTab by remember { mutableIntStateOf(0) }
                    if (showPlayerRanking && showTeamRanking) {
                        TabRow(selectedTabIndex = rankingTab, containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)) {
                            Tab(
                                selected = rankingTab == 0,
                                onClick = { rankingTab = 0 },
                                text = { Text("Giocatori", fontSize = 12.sp, fontWeight = if (rankingTab == 0) FontWeight.Bold else FontWeight.Normal) },
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Tab(
                                selected = rankingTab == 1,
                                onClick = { rankingTab = 1 },
                                text = { Text("Squadre", fontSize = 12.sp, fontWeight = if (rankingTab == 1) FontWeight.Bold else FontWeight.Normal) },
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    when {
                        showPlayerRanking && showTeamRanking && rankingTab == 1 -> TeamRankingList(s.teamRankings)
                        showTeamRanking -> TeamRankingList(s.teamRankings)
                        else -> PlayerRankingList(s.playerRankings)
                    }
                }
                1 -> MatchList(s.seasonMatches, s.seasonTeams)
                2 -> TeamList(s.seasonTeams) { team ->
                    vm.navigateTo(Screen.TeamDetail(league, season, competition, team))
                    vm.loadTeamDetailData(season.id, competition.id, team.id)
                }
                3 -> UserList(s.seasonUsers) { user ->
                    vm.navigateTo(Screen.PlayerDetail(league, season, competition, user))
                    vm.loadPlayerDetailData(season.id, competition.id, user.userId)
                }
            }
        }
    }
}

@Composable
fun PlayerRankingList(rankings: List<PlayerRankingResponse>) {
    val scrollState = rememberScrollState()
    
    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier
                .wrapContentWidth()
                .horizontalScroll(scrollState)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", Modifier.width(30.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
            Text("Giocatore", Modifier.width(180.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Rat", Modifier.width(45.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text("P", Modifier.width(30.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text("GF", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text("GS", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text("CF", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text("CS", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
        }
        HorizontalDivider()

        LazyColumn(Modifier.fillMaxSize()) {
            items(rankings.indices.toList()) { index ->
                val player = rankings[index]
                Row(
                    Modifier
                        .wrapContentWidth()
                        .horizontalScroll(scrollState)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}", Modifier.width(30.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        player.username,
                        Modifier.width(180.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("${player.rating}", Modifier.width(45.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${player.matchesPlayed}", Modifier.width(30.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${player.goalsFor}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = if (isSystemInDarkTheme()) Color(0xFF4CAF50) else Color(0xFF2E7D32))
                    Text("${player.goalsAgainst}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = if (isSystemInDarkTheme()) Color(0xFFEF5350) else Color(0xFFD32F2F))
                    Text("${player.cappottiGiven}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${player.cappottiReceived}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun TeamRankingList(rankings: List<TeamRankingResponse>) {
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier
                .wrapContentWidth()
                .horizontalScroll(scrollState)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", Modifier.width(30.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
            Text("Squadra", Modifier.width(200.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Rat", Modifier.width(45.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text("P", Modifier.width(30.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text("GF", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text("GS", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text("CF", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            Text("CS", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
        }
        HorizontalDivider()

        LazyColumn(Modifier.fillMaxSize()) {
            items(rankings.indices.toList()) { index ->
                val team = rankings[index]
                Column {
                    Row(
                        Modifier
                            .wrapContentWidth()
                            .horizontalScroll(scrollState)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}", Modifier.width(30.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.width(200.dp)) {
                            Text(team.teamName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "${team.playerAUsername} & ${team.playerBUsername}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("${team.rating}", Modifier.width(45.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("${team.matchesPlayed}", Modifier.width(30.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("${team.goalsFor}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = if (isSystemInDarkTheme()) Color(0xFF4CAF50) else Color(0xFF2E7D32))
                        Text("${team.goalsAgainst}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = if (isSystemInDarkTheme()) Color(0xFFEF5350) else Color(0xFFD32F2F))
                        Text("${team.cappottiGiven}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("${team.cappottiReceived}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MatchList(matches: List<MatchResponse>, teams: List<TeamResponse>) {
    val groupedMatches = remember(matches) {
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
                MatchItem(match, teams)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun MatchItem(match: MatchResponse, teams: List<TeamResponse>) {
    val teamA = teams.find { it.id == match.teamAId }
    val teamB = teams.find { it.id == match.teamBId }

    val playerA1 = teamA?.playerAUsername
    val playerA2 = teamA?.playerBUsername
    val playerB1 = teamB?.playerAUsername
    val playerB2 = teamB?.playerBUsername

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team A
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = match.teamAName ?: "Team A",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    playerA1?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                    playerA2?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                    
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${match.scoreA}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    match.teamARatingAfter?.let { rating ->
                        Text(
                            text = "$rating pt",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
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
                        text = match.teamBName ?: "Team B",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    playerB1?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                    playerB2?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${match.scoreB}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

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

@Composable
fun TeamList(teams: List<TeamResponse>, onTeamClick: (TeamResponse) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(teams) { team ->
            ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onTeamClick(team) }) {
                ListItem(
                    headlineContent = { Text(team.name, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("${team.playerAUsername} & ${team.playerBUsername}") },
                    trailingContent = { Text("Rat: ${team.rating ?: 0}") }
                )
            }
        }
    }
}

@Composable
fun UserList(users: List<LeagueUserResponse>, onUserClick: (LeagueUserResponse) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(users) { user ->
            ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onUserClick(user) }) {
                ListItem(
                    headlineContent = { Text(user.username, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(user.email ?: "") },
                    trailingContent = { Text("Rat: ${user.rating}") }
                )
            }
        }
    }
}
