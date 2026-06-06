package com.biliardino.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Search
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
import com.biliardino.model.*
import com.biliardino.ui.Screen
import com.biliardino.ui.components.TournamentBracketView
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionStatisticsScreen(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse, s: UiState, vm: AppViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showResultDialogByBracket by remember { mutableStateOf<MatchResponse?>(null) }

    // Carichiamo i dati all'avvio
    LaunchedEffect(competition.id) {
        vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
    }

    Column(Modifier.fillMaxSize()) {
        val isTeamMatch = competition.matchType == "TEAM"
        val isCup = competition.type == "CUP"
        
        val tabs = if (isCup) {
            listOf("Tabellone", "Chat", "Squadre")
        } else {
            if (isTeamMatch) listOf("Classifica", "Chat", "Squadre") else listOf("Classifica", "Giocatori", "Chat", "Squadre")
        }

        LaunchedEffect(selectedTab) {
            val chatTabIndex = if (isCup) 1 else if (isTeamMatch) 1 else 2
            if (selectedTab == chatTabIndex) {
                vm.loadCompetitionComments(competition.id)
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { 
                        selectedTab = index
                        searchQuery = "" 
                    },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Normal) },
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val isSearchVisible = if (isCup) selectedTab == 2 else if (isTeamMatch) selectedTab == 2 else (selectedTab == 1 || selectedTab == 3)

        if (isSearchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                placeholder = { Text("Cerca...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }

        Box(Modifier.weight(1f)) {
            if (isCup) {
                when (selectedTab) {
                    0 -> TournamentBracketView(
                        matches = s.seasonMatches,
                        onMatchClick = { match ->
                            vm.loadMatchComments(match.id)
                            vm.navigateTo(Screen.MatchDetail(match))
                        }
                    )
                    1 -> CompetitionChatTab(competition.id, s, vm)
                    2 -> {
                        val filteredTeams = s.seasonTeams
                            .filter { it.name.contains(searchQuery, ignoreCase = true) }
                            .sortedBy { it.name }
                        SeasonTeamsScreen(league, season, competition, s.copy(seasonTeams = filteredTeams), vm)
                    }
                }
            } else {
                if (isTeamMatch) {
                    when (selectedTab) {
                        0 -> Column { TeamRankingList(s.teamRankings, competition, vm) }
                        1 -> CompetitionChatTab(competition.id, s, vm)
                        2 -> {
                            val filteredTeams = s.seasonTeams
                                .filter { it.name.contains(searchQuery, ignoreCase = true) }
                                .sortedByDescending { it.points ?: 0 }
                            SeasonTeamsScreen(league, season, competition, s.copy(seasonTeams = filteredTeams), vm)
                        }
                    }
                } else {
                    when (selectedTab) {
                        0 -> Column {
                            var rankingTab by remember { mutableIntStateOf(0) }
                            val showPlayerRanking = competition.rankingMode != "TEAM"
                            val showTeamRanking = competition.rankingMode != "PLAYER"
                            
                            if (showPlayerRanking && showTeamRanking) {
                                SecondaryTabRow(selectedTabIndex = rankingTab, containerColor = MaterialTheme.colorScheme.surface) {
                                    Tab(selected = rankingTab == 0, onClick = { rankingTab = 0 }, text = { Text("GIOCATORI", style = MaterialTheme.typography.labelLarge, fontWeight = if (rankingTab == 0) FontWeight.Black else FontWeight.Bold) })
                                    Tab(selected = rankingTab == 1, onClick = { rankingTab = 1 }, text = { Text("SQUADRE", style = MaterialTheme.typography.labelLarge, fontWeight = if (rankingTab == 1) FontWeight.Black else FontWeight.Bold) })
                                }
                            }
                            when {
                                showPlayerRanking && showTeamRanking && rankingTab == 0 -> PlayerRankingList(s.playerRankings, vm)
                                showPlayerRanking && showTeamRanking -> TeamRankingList(s.teamRankings, competition, vm)
                                showPlayerRanking -> PlayerRankingList(s.playerRankings, vm)
                                else -> TeamRankingList(s.teamRankings, competition, vm)
                            }
                        }
                        1 -> {
                            val filteredUsers = s.seasonUsers
                                .filter { it.username.contains(searchQuery, ignoreCase = true) || (it.email?.contains(searchQuery, ignoreCase = true) == true) }
                                .sortedByDescending { it.rating }
                            UserList(filteredUsers) { user ->
                                vm.loadPlayerProfile(user.userId)
                                vm.navigateTo(Screen.PlayerProfile(user.userId, user.username))
                            }
                        }
                        2 -> CompetitionChatTab(competition.id, s, vm)
                        3 -> {
                            val filteredTeams = s.seasonTeams
                                .filter { it.name.contains(searchQuery, ignoreCase = true) || (it.playerAUsername?.contains(searchQuery, ignoreCase = true) == true) || (it.playerBUsername?.contains(searchQuery, ignoreCase = true) == true) }
                                .sortedByDescending { it.rating ?: 0 }
                            SeasonTeamsScreen(league, season, competition, s.copy(seasonTeams = filteredTeams), vm)
                        }
                    }
                }
            }
        }
    }

    if (showResultDialogByBracket != null) {
        com.biliardino.ui.components.MatchResultDialog(
            match = showResultDialogByBracket!!,
            onDismiss = { showResultDialogByBracket = null },
            onConfirm = { sA, sB ->
                vm.updateMatchResult(competition.id, showResultDialogByBracket!!.id, sA, sB)
                showResultDialogByBracket = null
            }
        )
    }
}

@Composable
fun PlayerRankingList(rankings: List<PlayerRankingResponse>, vm: AppViewModel) {
    var sortField by remember { mutableStateOf(RankingSortField.Rating) }
    var descending by remember { mutableStateOf(true) }
    val horizontalScrollState = rememberScrollState()

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

    RankingListScaffold(emptyText = "La classifica verrà popolata dopo le prime partite.", isEmpty = rankings.isEmpty(), isTeamMatch = false, scrollState = horizontalScrollState) {
        item { RankingSortControls(sortField = sortField, descending = descending, onSortFieldChange = { sortField = it }, onToggleDirection = { descending = !descending }) }
        itemsIndexed(sortedRankings, key = { _, p -> p.userId }) { index, player ->
            RankingRow(
                position = index + 1,
                title = player.username ?: "Utente",
                subtitle = null,
                mainValue = player.rating.toString(),
                stats = listOf("PG" to player.matchesPlayed.toString(), "GF" to player.goalsFor.toString(), "GS" to player.goalsAgainst.toString(), "CF" to player.cappottiGiven.toString(), "CS" to player.cappottiReceived.toString()),
                scrollState = horizontalScrollState,
                onRowClick = {
                    vm.loadPlayerProfile(player.userId)
                    vm.navigateTo(Screen.PlayerProfile(player.userId, player.username ?: "Giocatore"))
                }
            )
        }
    }
}

@Composable
fun TeamRankingList(rankings: List<TeamRankingResponse>, competition: CompetitionResponse, vm: AppViewModel) {
    val isTeamType = competition.matchType == "TEAM"
    var sortField by remember { mutableStateOf(if (isTeamType) RankingSortField.Points else RankingSortField.Rating) }
    var descending by remember { mutableStateOf(true) }
    val horizontalScrollState = rememberScrollState()

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

    RankingListScaffold(emptyText = "La classifica squadre verrà popolata dopo le prime partite.", isEmpty = rankings.isEmpty(), isTeamMatch = isTeamType, scrollState = horizontalScrollState) {
        item { RankingSortControls(sortField = sortField, descending = descending, onSortFieldChange = { sortField = it }, onToggleDirection = { descending = !descending }, matchType = competition.matchType) }
        itemsIndexed(sortedRankings, key = { _, team -> team.teamId }) { index, team ->
            val isTeam = team.playerAId == null
            val isDouble = team.playerAId != null && team.playerBId != null
            val isSingle = team.playerAId != null && team.playerBId == null

            val title = if (isDouble || isSingle) team.playerAUsername ?: team.teamName else team.teamName
            val subtitle = if (isDouble) team.playerBUsername else null

            val mainValue = if (isTeamType) team.points.toString() else team.rating.toString()
            val stats = if (isTeamType) {
                listOf("PG" to team.matchesPlayed.toString(), "V" to team.wins.toString(), "N" to team.draws.toString(), "P" to team.losses.toString(), "GF" to team.goalsFor.toString(), "GS" to team.goalsAgainst.toString(), "DR" to team.goalDifference.toString())
            } else {
                listOf("PG" to team.matchesPlayed.toString(), "GF" to team.goalsFor.toString(), "GS" to team.goalsAgainst.toString(), "CF" to (team.cappottiGiven?.toString() ?: "0"), "CS" to (team.cappottiReceived?.toString() ?: "0"))
            }

            RankingRow(
                position = index + 1,
                title = title,
                subtitle = subtitle,
                mainValue = mainValue,
                stats = stats,
                scrollState = horizontalScrollState,
                onTitleClick = if (!isTeam) { { team.playerAId?.let { vm.loadPlayerProfile(it); vm.navigateTo(Screen.PlayerProfile(it, team.playerAUsername ?: "Giocatore")) } } } else null,
                onSubtitleClick = if (isDouble) { { team.playerBId?.let { vm.loadPlayerProfile(it); vm.navigateTo(Screen.PlayerProfile(it, team.playerBUsername ?: "Giocatore")) } } } else null,
                onRowClick = if (isTeam) { { vm.loadTeamProfile(team.teamId); vm.navigateTo(Screen.TeamProfile(team.teamId, team.teamName)) } } else null
            )
        }
    }
}

@Composable
private fun RankingListScaffold(emptyText: String, isEmpty: Boolean, isTeamMatch: Boolean, scrollState: androidx.compose.foundation.ScrollState, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    if (isEmpty) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = MaterialTheme.shapes.extraLarge) {
                Text("Nessun dato ancora disponibile per questa classifica. $emptyText", modifier = Modifier.padding(32.dp).fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            RankingHeader(isTeamMatch, scrollState)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp), content = content)
        }
    }
}

@Composable
private fun RankingHeader(isTeamMatch: Boolean, scrollState: androidx.compose.foundation.ScrollState) {
    Surface(color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("POS", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("NOME", modifier = Modifier.width(150.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.weight(1f).horizontalScroll(scrollState).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val headers = if (isTeamMatch) listOf("PG", "V", "N", "P", "GF", "GS", "DR") else listOf("PG", "GF", "GS", "CF", "CS")
                headers.forEach { header ->
                    Text(text = header, modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            Text(text = if (isTeamMatch) "PTS" else "RAT", modifier = Modifier.width(55.dp).padding(end = 12.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun RankingRow(position: Int, title: String, subtitle: String?, mainValue: String, stats: List<Pair<String, String>>, scrollState: androidx.compose.foundation.ScrollState, onTitleClick: (() -> Unit)? = null, onSubtitleClick: (() -> Unit)? = null, onRowClick: (() -> Unit)? = null) {
    val accent = rankAccent(position)
    Surface(modifier = Modifier.fillMaxWidth().then(if (onRowClick != null) Modifier.clickable(onClick = onRowClick) else Modifier)) {
        Column {
            Row(modifier = Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = position.toString(), modifier = Modifier.width(32.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = if (position <= 3) accent else MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.width(150.dp)) {
                        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (position <= 3) FontWeight.ExtraBold else FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier, color = if (onTitleClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        if (!subtitle.isNullOrBlank()) {
                            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = if (onSubtitleClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = if (onSubtitleClick != null) Modifier.clickable(onClick = onSubtitleClick) else Modifier)
                        }
                    }
                }
                Row(modifier = Modifier.weight(1f).horizontalScroll(scrollState).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    stats.forEach { (_, value) ->
                        Text(text = value, modifier = Modifier.width(24.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    }
                }
                Text(text = mainValue, modifier = Modifier.width(55.dp).padding(end = 12.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black, textAlign = TextAlign.End, color = if (position <= 3) accent else MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionChatTab(competitionId: Long, s: UiState, vm: AppViewModel) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(s.competitionComments.size) {
        if (s.competitionComments.isNotEmpty()) {
            listState.animateScrollToItem(s.competitionComments.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = s.loading,
            onRefresh = { vm.loadCompetitionComments(competitionId) },
            modifier = Modifier.weight(1f)
        ) {
            if (s.competitionComments.isEmpty() && !s.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nessun messaggio in questa competizione. Inizia tu!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val sortedComments = remember(s.competitionComments) {
                    s.competitionComments.sortedBy { it.createdAt }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(sortedComments, key = { _, c -> c.id }) { _, comment ->
                        val isMe = comment.userId == s.currentUser?.userId
                        CommentBubble(comment.username, comment.message, comment.createdAt, isMe)
                    }
                }
            }
        }

        Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp).imePadding(), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { if (it.length <= 1000) messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Scrivi un messaggio...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            vm.addCompetitionComment(competitionId, messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Invia", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private enum class RankingSortField(val label: String) {
    Rating("Rating"), Points("Punti"), GoalsFor("G. Fatti"), GoalsAgainst("G. Subiti"), GoalDifference("Diff. Reti"),
    Played("Partite"), Wins("Vinte"), Draws("Pareggiate"), Losses("Perse"), CappottiGiven("C. Fatti"), CappottiReceived("C. Subiti")
}

private fun <T> List<T>.sortedByField(field: RankingSortField, descending: Boolean, selector: (T, RankingSortField) -> Int): List<T> {
    val nameSelector: (T) -> String = { item ->
        when (item) {
            is PlayerRankingResponse -> item.username ?: ""
            is TeamRankingResponse -> item.teamName
            else -> ""
        }
    }
    val comparator = if (descending) compareByDescending<T> { selector(it, field) }.thenBy(nameSelector)
    else compareBy<T> { selector(it, field) }.thenBy(nameSelector)
    return sortedWith(comparator)
}

@Composable
private fun RankingSortControls(sortField: RankingSortField, descending: Boolean, onSortFieldChange: (RankingSortField) -> Unit, onToggleDirection: () -> Unit, matchType: String? = null) {
    val isTeamType = matchType == "TEAM"
    val visibleFields = if (isTeamType) listOf(RankingSortField.Points, RankingSortField.GoalDifference, RankingSortField.Played, RankingSortField.Wins)
    else listOf(RankingSortField.Rating, RankingSortField.Played, RankingSortField.GoalsFor)

    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Sort, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                visibleFields.forEach { field ->
                    FilterChip(selected = sortField == field, onClick = { onSortFieldChange(field) }, label = { Text(field.label, fontSize = 11.sp) }, shape = MaterialTheme.shapes.small)
                }
                IconButton(onClick = onToggleDirection, modifier = Modifier.size(32.dp)) {
                    Icon(if (descending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun rankAccent(rank: Int): Color = when (rank) {
    1 -> Color(0xFFFFD700)
    2 -> Color(0xFFC0C0C0)
    3 -> Color(0xFFCD7F32)
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun UserList(users: List<LeagueUserResponse>, onUserClick: (LeagueUserResponse) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(users) { _, user ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onUserClick(user) }, shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
                ListItem(colors = ListItemDefaults.colors(containerColor = Color.Transparent), headlineContent = { Text(user.username, fontWeight = FontWeight.Black) }, supportingContent = { Text(user.email ?: "Nessuna email") }, trailingContent = {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                        Text("Rat: ${user.rating}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                })
            }
        }
    }
}
