package com.biliardino.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biliardino.model.CompetitionResponse
import com.biliardino.model.LeagueResponse
import com.biliardino.model.SeasonResponse
import com.biliardino.ui.Screen
import com.biliardino.util.DateUtils
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
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            vm.loadTrophies(season.id)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Competizioni") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Trofei") })
            }

            Box(Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    CompetitionsTab(league, season, s, vm) { competitionToJoin = it }
                } else {
                    TrophiesTab(league, season, s, vm)
                }
            }
        }

        if (selectedTab == 0 && (s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER")) {
            if (season.active != false) {
                FloatingActionButton(
                    onClick = { vm.navigateTo(Screen.CreateCompetition(league, season)) },
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

    if (competitionToJoin != null) {
        val isAdminOrOwner = s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER"
        
        AlertDialog(
            onDismissRequest = { competitionToJoin = null },
            title = { Text("Partecipa alla Competizione", fontWeight = FontWeight.Bold) },
            text = { Text("Vuoi iscriverti alla competizione \"${competitionToJoin?.name}\" per iniziare a registrare le tue partite?") },
            confirmButton = {
                if (isAdminOrOwner) {
                    TextButton(
                        onClick = {
                            competitionToJoin?.let { vm.selectCompetition(league, season, it) }
                            competitionToJoin = null
                        }
                    ) {
                        Text("Entra come Admin")
                    }
                }
                Button(
                    onClick = {
                        competitionToJoin?.let { comp ->
                            if (comp.matchType == "TEAM") {
                                vm.navigateTo(Screen.JoinTeam(league, season, comp))
                            } else {
                                vm.joinCompetition(league, season, comp.id)
                            }
                        }
                        competitionToJoin = null
                    },
                    enabled = competitionToJoin?.registrationOpen != false,
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Partecipa")
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompetitionsTab(
    league: LeagueResponse,
    season: SeasonResponse,
    s: UiState,
    vm: AppViewModel,
    onJoin: (CompetitionResponse) -> Unit
) {
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
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        val groupedCompetitions = remember(s.competitions) {
            s.competitions.groupBy { it.sportName ?: "Altro" }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LeagueSectionHeader(
                    title = "Competizioni disponibili",
                    count = s.competitions.size
                )
            }

            groupedCompetitions.forEach { (sport, competitions) ->
                stickyHeader {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ) {
                        Text(
                            text = sport.uppercase(),
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                }
                items(competitions) { competition ->
                    CompetitionCard(
                        competition = competition,
                        onClick = {
                            if (competition.currentUserJoined) {
                                vm.selectCompetition(league, season, competition)
                            } else {
                                onJoin(competition)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TrophiesTab(
    league: LeagueResponse,
    season: SeasonResponse,
    s: UiState,
    vm: AppViewModel
) {
    if (s.trophies.isEmpty() && !s.loading) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Nessun trofeo disponibile per questa stagione.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Trofei stagione",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(s.trophies) { trophy ->
                val comp = s.competitions.find { it.id == trophy.competitionId }
                TrophyCard(trophy, competition = comp) {
                    if (comp != null) {
                        vm.selectCompetition(league, season, comp)
                    }
                }
            }
        }
    }
}

@Composable
fun TrophyCard(
    trophy: com.biliardino.model.TrophyResponse,
    showWinner: Boolean = true,
    competition: CompetitionResponse? = null,
    onClick: () -> Unit
) {
    val sportLabel = competition?.sportName?.takeIf { it.isNotBlank() }
        ?: trophy.sportName?.takeIf { it.isNotBlank() }
        ?: "Sport non definito"
    val typeLabel = competitionTypeLabel(competition?.type ?: trophy.competitionType)
    val rankingLabel = rankingModeLabel(competition?.rankingMode)
    val matchTypeLabel = matchTypeLabel(competition?.matchType ?: trophy.matchType)
    val dateLabel = trophy.closedAt?.let { DateUtils.formatDate(it) }
    val teamWinnerName = trophy.winnerTeamName
        ?.takeIf { trophy.winnerTeamId != null && it.isNotBlank() }
        ?.trim()
    val playerWinnerName = trophy.winnerUserName
        ?.takeIf { trophy.winnerUserId != null && it.isNotBlank() }
        ?.trim()

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.28f))
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color(0xFFFFB300)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    trophy.competitionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            if (showWinner) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (teamWinnerName != null) {
                        TrophyWinnerBlock(
                            icon = Icons.Default.EmojiEvents,
                            title = "Squadra vincitrice",
                            name = teamWinnerName,
                            color = Color(0xFFFFB300)
                        )
                    }
                    if (playerWinnerName != null) {
                        TrophyWinnerBlock(
                            icon = Icons.Default.MilitaryTech,
                            title = "Miglior giocatore",
                            name = playerWinnerName,
                            color = Color(0xFFF57C00)
                        )
                    }
                    if (teamWinnerName == null && playerWinnerName == null) {
                        Text(
                            "Vincitore da definire",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrophyInfoTile(
                        icon = sportIcon(sportLabel),
                        title = "Sport",
                        value = sportLabel,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    TrophyInfoTile(
                        icon = Icons.Default.Flag,
                        title = "Tipo",
                        value = typeLabel,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrophyInfoTile(
                        icon = rankingIcon(competition?.rankingMode),
                        title = "Classifica",
                        value = rankingLabel,
                        color = Color(0xFF7B1FA2),
                        modifier = Modifier.weight(1f)
                    )
                    TrophyInfoTile(
                        icon = Icons.Default.Groups,
                        title = "Formato",
                        value = matchTypeLabel,
                        color = Color(0xFFF57C00),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (dateLabel != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Chiuso il $dateLabel",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TrophyWinnerBlock(icon: ImageVector, title: String, name: String, color: Color) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TrophyInfoTile(
    icon: ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = 0.10f)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    value,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun competitionTypeLabel(type: String?): String = when (type?.uppercase()) {
    "LEAGUE" -> "Campionato"
    "CUP", "TOURNAMENT" -> "Torneo"
    null, "" -> "Tipo non definito"
    else -> type
}

private fun matchTypeLabel(matchType: String?): String = when (matchType?.uppercase()) {
    "SINGLE" -> "Singolo"
    "DOUBLE" -> "Doppio"
    "TEAM" -> "Squadra"
    null, "" -> "Non definito"
    else -> matchType
}

private fun rankingModeLabel(rankingMode: String?): String = when (rankingMode?.uppercase()) {
    "PLAYER" -> "Giocatori"
    "TEAM" -> "Squadre"
    "BOTH" -> "Entrambe"
    null, "" -> "Non definita"
    else -> rankingMode
}

private fun rankingIcon(rankingMode: String?): ImageVector = when (rankingMode?.uppercase()) {
    "PLAYER" -> Icons.Default.Person
    "TEAM" -> Icons.Default.Groups
    else -> Icons.Default.Groups
}

private fun sportIcon(sportName: String): ImageVector {
    val normalized = sportName.trim().lowercase()
    return when {
        normalized.contains("calcio") -> Icons.Default.SportsSoccer
        normalized.contains("tennis") || normalized.contains("padel") -> Icons.Default.SportsTennis
        normalized.contains("biliardino") || normalized.contains("biliardo") -> Icons.Default.GridOn
        else -> Icons.Default.Sports
    }
}

@Composable
fun CompetitionCard(competition: CompetitionResponse, onClick: () -> Unit) {
    val isLeague = competition.type == "LEAGUE"
    val isActive = competition.active ?: (competition.status == "ACTIVE" || competition.status == null)
    val typeLabel = if (isLeague) "Campionato" else "Torneo"
    val sportLabel = competition.sportName ?: "Sport"
    val accentColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val actionColor = if (competition.currentUserJoined) accentColor else Color(0xFFF57C00)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isActive) 0.22f else 0.14f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = accentColor.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = if (isLeague) Icons.Default.Groups else Icons.Default.MilitaryTech,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp).fillMaxSize(),
                        tint = accentColor
                    )
                }

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = competition.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        CompetitionStatusChip(isActive)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompetitionInfoChip(sportLabel, MaterialTheme.colorScheme.secondary)
                        CompetitionInfoChip(typeLabel, accentColor)
                        RegistrationStatusChip(competition.registrationOpen)
                    }
                }
            }

            if (!competition.startDate.isNullOrBlank() || !competition.endDate.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CompetitionDatePill(
                        label = "Inizio",
                        value = competition.startDate,
                        modifier = Modifier.weight(1f)
                    )
                    CompetitionDatePill(
                        label = "Fine",
                        value = competition.endDate,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (competition.currentUserJoined) Icons.Default.ArrowCircleRight else Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (competition.currentUserJoined) "Apri campionato" else "Tocca per partecipare",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = actionColor
                )
                Spacer(Modifier.weight(1f))
                if (!competition.currentUserJoined) {
                    CompetitionNotJoinedChip()
                } else {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RegistrationStatusChip(isOpen: Boolean) {
    Surface(
        color = (if (isOpen) Color(0xFF4CAF50) else Color(0xFF757575)).copy(alpha = 0.12f),
        shape = androidx.compose.foundation.shape.CircleShape
    ) {
        Text(
            text = if (isOpen) "🟢 Iscrizioni aperte" else "🔒 Iscrizioni chiuse",
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (isOpen) Color(0xFF2E7D32) else Color(0xFF424242),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CompetitionInfoChip(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = androidx.compose.foundation.shape.CircleShape
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CompetitionStatusChip(isActive: Boolean) {
    Surface(
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = if (isActive) "In corso" else "Concluso",
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun CompetitionNotJoinedChip() {
    Surface(
        color = Color(0xFFF57C00).copy(alpha = 0.14f),
        shape = androidx.compose.foundation.shape.CircleShape
    ) {
        Text(
            text = "Non iscritto",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFF57C00),
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun CompetitionDatePill(label: String, value: String?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    DateUtils.formatDate(value),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
