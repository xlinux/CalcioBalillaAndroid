package com.biliardino.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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

@Composable
fun SeasonStatisticsScreen(league: LeagueResponse, season: SeasonResponse, s: UiState, vm: AppViewModel) {
    // Carichiamo i dati all'avvio se la lista è vuota
    LaunchedEffect(season.id) {
        vm.loadRankings(season.id)
    }

    Column(Modifier.fillMaxSize()) {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = listOf("Classifica", "Partite", "Squadre", "Giocatori")

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        Box(Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> Column {
                    var rankingTab by remember { mutableIntStateOf(0) }
                    TabRow(selectedTabIndex = rankingTab, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Tab(selected = rankingTab == 0, onClick = { rankingTab = 0 }, text = { Text("Giocatori", fontSize = 12.sp) })
                        Tab(selected = rankingTab == 1, onClick = { rankingTab = 1 }, text = { Text("Squadre", fontSize = 12.sp) })
                    }
                    if (rankingTab == 0) PlayerRankingList(s.playerRankings) else TeamRankingList(s.teamRankings)
                }
                1 -> MatchList(s.seasonMatches)
                2 -> TeamList(s.seasonTeams) { team ->
                    vm.navigateTo(Screen.TeamDetail(league, season, team))
                    vm.loadTeamDetailData(season.id, team.id)
                }
                3 -> UserList(s.seasonUsers) { user ->
                    vm.navigateTo(Screen.PlayerDetail(league, season, user))
                    vm.loadPlayerDetailData(season.id, user.userId)
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", Modifier.width(30.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Giocatore", Modifier.width(180.dp), fontWeight = FontWeight.Bold)
            Text("Rat", Modifier.width(45.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("P", Modifier.width(30.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("GF", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("GS", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("CF", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("CS", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
                    Text("${index + 1}", Modifier.width(30.dp), textAlign = TextAlign.Center, fontSize = 14.sp)
                    Text(
                        player.username,
                        Modifier.width(180.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    Text("${player.rating}", Modifier.width(45.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("${player.matchesPlayed}", Modifier.width(30.dp), textAlign = TextAlign.Center, fontSize = 14.sp)
                    Text("${player.goalsFor}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Text("${player.goalsAgainst}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                    Text("${player.cappottiGiven}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp)
                    Text("${player.cappottiReceived}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp)
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", Modifier.width(30.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Squadra", Modifier.width(200.dp), fontWeight = FontWeight.Bold)
            Text("Rat", Modifier.width(45.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("P", Modifier.width(30.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("GF", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("GS", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("CF", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("CS", Modifier.width(35.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
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
                        Text("${index + 1}", Modifier.width(30.dp), textAlign = TextAlign.Center, fontSize = 14.sp)
                        Column(Modifier.width(200.dp)) {
                            Text(team.teamName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "${team.playerAUsername} & ${team.playerBUsername}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("${team.rating}", Modifier.width(45.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("${team.matchesPlayed}", Modifier.width(30.dp), textAlign = TextAlign.Center, fontSize = 14.sp)
                        Text("${team.goalsFor}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        Text("${team.goalsAgainst}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                        Text("${team.cappottiGiven}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp)
                        Text("${team.cappottiReceived}", Modifier.width(35.dp), textAlign = TextAlign.Center, fontSize = 14.sp)
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun MatchList(matches: List<MatchResponse>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(matches.reversed()) { match ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(match.teamAName ?: "Team A", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${match.scoreA}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                            
                            match.teamARatingDelta?.let { delta ->
                                Text(
                                    text = "${if (delta >= 0) "+" else ""}$delta ELO",
                                    color = if (delta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text("VS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.outline)
                            if (match.cappotto == true) {
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        "CAPPOTTO",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                match.cappottoBonusApplied?.let { bonus ->
                                    Text(
                                        text = "+$bonus bonus",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(match.teamBName ?: "Team B", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${match.scoreB}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)

                            match.teamBRatingDelta?.let { delta ->
                                Text(
                                    text = "${if (delta >= 0) "+" else ""}$delta ELO",
                                    color = if (delta >= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    match.playedAt?.let { date ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = date.replace("T", " ").substring(0, 16),
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
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
