package com.biliardino.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Search
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
fun CompetitionStatisticsScreen(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse, s: UiState, vm: AppViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Carichiamo i dati all'avvio
    LaunchedEffect(competition.id) {
        vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
    }

    Column(Modifier.fillMaxSize()) {
        val isTeamMatch = competition.matchType == "TEAM"
        val tabs = if (isTeamMatch) listOf("Classifica", "Squadre") else listOf("Classifica", "Giocatori", "Squadre")
        val showPlayerRanking = competition.rankingMode != "TEAM" && !isTeamMatch
        val showTeamRanking = competition.rankingMode != "PLAYER"

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { 
                        selectedTab = index
                        if (index != 1 && index != 2) searchQuery = "" // Reset search when leaving players or teams tab
                    },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (selectedTab == 1 || selectedTab == 2) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                placeholder = { Text(if (selectedTab == 1) "Cerca giocatore..." else "Cerca squadra...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }

        Box(Modifier.weight(1f)) {
            val actualTab = if (isTeamMatch && selectedTab >= 1) selectedTab + 1 else selectedTab
            when (actualTab) {
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
                        showPlayerRanking && showTeamRanking && rankingTab == 0 -> PlayerRankingList(s.playerRankings)
                        showPlayerRanking && showTeamRanking -> TeamRankingList(s.teamRankings, competition)
                        showPlayerRanking -> PlayerRankingList(s.playerRankings)
                        else -> TeamRankingList(s.teamRankings, competition)
                    }
                }
                1 -> {
                    val filteredUsers = s.seasonUsers
                        .filter { it.username.contains(searchQuery, ignoreCase = true) || (it.email?.contains(searchQuery, ignoreCase = true) == true) }
                        .sortedByDescending { it.rating }
                    
                    UserList(filteredUsers) { user ->
                        vm.navigateTo(Screen.PlayerDetail(league, season, competition, user))
                        vm.loadPlayerDetailData(competition.id, user.userId)
                    }
                }
                2 -> {
                    val filteredTeams = s.seasonTeams
                        .filter { 
                            it.name.contains(searchQuery, ignoreCase = true) || 
                            (it.playerAUsername?.contains(searchQuery, ignoreCase = true) == true) || 
                            (it.playerBUsername?.contains(searchQuery, ignoreCase = true) == true)
                        }
                        .sortedByDescending { if (isTeamMatch) (it.points ?: 0) else (it.rating ?: 0) }
                        
                    SeasonTeamsScreen(league, season, competition, s.copy(seasonTeams = filteredTeams), vm)
                }
            }
        }
    }
}

@Composable
fun PlayerRankingList(rankings: List<PlayerRankingResponse>) {
    var sortField by remember { mutableStateOf(RankingSortField.Rating) }
    var descending by remember { mutableStateOf(true) }
    val sortedRankings = remember(rankings, sortField, descending) {
        rankings.sortedByField(sortField, descending) { row, field ->
            when (field) {
                RankingSortField.Rating -> row.rating
                RankingSortField.GoalsFor -> row.goalsFor
                RankingSortField.GoalsAgainst -> row.goalsAgainst
                RankingSortField.Played -> row.matchesPlayed
                RankingSortField.CappottiGiven -> row.cappottiGiven
                RankingSortField.CappottiReceived -> row.cappottiReceived
                else -> 0
            }
        }
    }

    RankingListScaffold(emptyText = "La classifica verrà popolata dopo le prime partite registrate.", isEmpty = rankings.isEmpty()) {
        item {
            RankingSortControls(
                sortField = sortField,
                descending = descending,
                onSortFieldChange = { sortField = it },
                onToggleDirection = { descending = !descending }
            )
        }
        itemsIndexed(sortedRankings, key = { _, player -> player.userId }) { index, player ->
            RankingCard(
                position = index + 1,
                title = player.username ?: "Utente",
                subtitle = "${player.matchesPlayed} partite giocate",
                rating = player.rating,
                stats = listOf(
                    RankingStatItem("PG", player.matchesPlayed.toString(), RankingStatTone.Purple),
                    RankingStatItem("GF", player.goalsFor.toString(), RankingStatTone.Blue),
                    RankingStatItem("GS", player.goalsAgainst.toString(), RankingStatTone.Red),
                    RankingStatItem("CF", player.cappottiGiven.toString(), RankingStatTone.Orange),
                    RankingStatItem("CS", player.cappottiReceived.toString(), RankingStatTone.Neutral)
                )
            )
        }
    }
}

@Composable
fun TeamRankingList(rankings: List<TeamRankingResponse>, competition: CompetitionResponse) {
    val isTeamType = competition.matchType == "TEAM"
    var sortField by remember { mutableStateOf(if (isTeamType) RankingSortField.Points else RankingSortField.Rating) }
    var descending by remember { mutableStateOf(true) }
    val sortedRankings = remember(rankings, sortField, descending) {
        rankings.sortedByField(sortField, descending) { row, field ->
            when (field) {
                RankingSortField.Rating -> row.rating
                RankingSortField.GoalsFor -> row.goalsFor
                RankingSortField.GoalsAgainst -> row.goalsAgainst
                RankingSortField.Played -> row.matchesPlayed
                RankingSortField.CappottiGiven -> row.cappottiGiven ?: 0
                RankingSortField.CappottiReceived -> row.cappottiReceived ?: 0
                RankingSortField.Wins -> row.wins
                RankingSortField.Draws -> row.draws
                RankingSortField.Losses -> row.losses
                RankingSortField.Points -> row.points
                RankingSortField.GoalDifference -> row.goalDifference
            }
        }
    }

    RankingListScaffold(emptyText = "La classifica squadre verrà popolata dopo le prime partite registrate.", isEmpty = rankings.isEmpty()) {
        item {
            RankingSortControls(
                sortField = sortField,
                descending = descending,
                onSortFieldChange = { sortField = it },
                onToggleDirection = { descending = !descending },
                matchType = competition.matchType
            )
        }
        itemsIndexed(sortedRankings, key = { _, team -> team.teamId }) { index, team ->
            val subtitle = when (competition.matchType) {
                "TEAM" -> team.playerAUsername ?: ""
                "SINGLE" -> team.playerAUsername ?: "Utente"
                else -> {
                    val playerA = team.playerAUsername ?: "Utente"
                    val playerB = team.playerBUsername ?: "Utente"
                    "$playerA + $playerB"
                }
            }

            val stats = mutableListOf<RankingStatItem>()
            stats.add(RankingStatItem("PG", team.matchesPlayed.toString(), RankingStatTone.Purple))
            
            if (isTeamType) {
                stats.add(RankingStatItem("V", team.wins.toString(), RankingStatTone.Blue))
                stats.add(RankingStatItem("N", team.draws.toString(), RankingStatTone.Neutral))
                stats.add(RankingStatItem("P", team.losses.toString(), RankingStatTone.Red))
                stats.add(RankingStatItem("GF", team.goalsFor.toString(), RankingStatTone.Blue))
                stats.add(RankingStatItem("GS", team.goalsAgainst.toString(), RankingStatTone.Red))
                stats.add(RankingStatItem("Diff", team.goalDifference.toString(), RankingStatTone.Neutral))
                stats.add(RankingStatItem("Punti", team.points.toString(), RankingStatTone.Orange))
            } else {
                stats.add(RankingStatItem("GF", team.goalsFor.toString(), RankingStatTone.Blue))
                stats.add(RankingStatItem("GS", team.goalsAgainst.toString(), RankingStatTone.Red))
                team.cappottiGiven?.let { stats.add(RankingStatItem("CF", it.toString(), RankingStatTone.Orange)) }
                team.cappottiReceived?.let { stats.add(RankingStatItem("CS", it.toString(), RankingStatTone.Neutral)) }
            }

            RankingCard(
                position = index + 1,
                title = team.teamName,
                subtitle = subtitle,
                rating = if (isTeamType) null else team.rating,
                stats = stats
            )
        }
    }
}

@Composable
private fun RankingListScaffold(
    emptyText: String,
    isEmpty: Boolean,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    if (isEmpty) {
        Box(
            Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    "Nessun dato\n$emptyText",
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

private enum class RankingSortField(val label: String) {
    Rating("Rating"),
    Points("Punti"),
    GoalsFor("GF"),
    GoalsAgainst("GS"),
    GoalDifference("Diff"),
    Played("PG"),
    Wins("V"),
    Draws("N"),
    Losses("P"),
    CappottiGiven("CF"),
    CappottiReceived("CS")
}

private fun <T> List<T>.sortedByField(
    field: RankingSortField,
    descending: Boolean,
    selector: (T, RankingSortField) -> Int
): List<T> {
    val nameSelector: (T) -> String = { item ->
        when (item) {
            is PlayerRankingResponse -> item.username ?: ""
            is TeamRankingResponse -> item.teamName
            else -> ""
        }
    }
    val comparator = if (descending) {
        compareByDescending<T> { selector(it, field) }.thenBy(nameSelector)
    } else {
        compareBy<T> { selector(it, field) }.thenBy(nameSelector)
    }
    return sortedWith(comparator)
}

private enum class RankingStatTone {
    Purple,
    Blue,
    Red,
    Orange,
    Neutral
}

private data class RankingStatItem(
    val label: String,
    val value: String,
    val tone: RankingStatTone
)

@Composable
private fun RankingSortControls(
    sortField: RankingSortField,
    descending: Boolean,
    onSortFieldChange: (RankingSortField) -> Unit,
    onToggleDirection: () -> Unit,
    matchType: String? = null
) {
    val isTeamType = matchType == "TEAM"
    val visibleFields = if (isTeamType) {
        listOf(RankingSortField.Points, RankingSortField.GoalDifference, RankingSortField.Played, RankingSortField.Wins, RankingSortField.Draws, RankingSortField.Losses, RankingSortField.GoalsFor, RankingSortField.GoalsAgainst)
    } else {
        listOf(RankingSortField.Rating, RankingSortField.Played, RankingSortField.GoalsFor, RankingSortField.GoalsAgainst, RankingSortField.CappottiGiven, RankingSortField.CappottiReceived)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            visibleFields.forEach { field ->
                FilterChip(
                    selected = sortField == field,
                    onClick = { onSortFieldChange(field) },
                    label = { Text(field.label) }
                )
            }
        }
        OutlinedButton(
            onClick = onToggleDirection,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                if (descending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (descending) "Decrescente" else "Crescente")
        }
    }
}

@Composable
private fun RankingCard(
    position: Int,
    title: String,
    subtitle: String,
    rating: Int?,
    stats: List<RankingStatItem>
) {
    val accent = rankAccent(position)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RankingPositionBadge(position, accent)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (rating != null) {
                    RatingBlock(rating)
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stats.forEach { RankingMetricPill(it) }
            }
        }
    }
}

@Composable
private fun RankingPositionBadge(position: Int, accent: Color) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.16f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                "#$position",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

@Composable
private fun RatingBlock(rating: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "Rating",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                rating.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RankingMetricPill(stat: RankingStatItem) {
    val color = stat.tone.metricColor()
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stat.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.width(5.dp))
            Text(
                stat.value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RankingStatTone.metricColor(): Color = when (this) {
    RankingStatTone.Purple -> MaterialTheme.colorScheme.tertiary
    RankingStatTone.Blue -> MaterialTheme.colorScheme.primary
    RankingStatTone.Red -> MaterialTheme.colorScheme.error
    RankingStatTone.Orange -> Color(0xFFF57C00)
    RankingStatTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun rankAccent(rank: Int): Color = when (rank) {
    1 -> Color(0xFFFFC107)
    2 -> Color(0xFF9E9E9E)
    3 -> Color(0xFFF57C00)
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun UserList(users: List<LeagueUserResponse>, onUserClick: (LeagueUserResponse) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(users) { _, user ->
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
