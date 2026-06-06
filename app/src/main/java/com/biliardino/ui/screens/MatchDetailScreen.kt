package com.biliardino.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CalendarMonth
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
import com.biliardino.util.DateUtils
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun MatchDetailScreen(match: MatchResponse, s: UiState, vm: AppViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Dettaglio") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Chat") })
        }
        
        when (selectedTab) {
            0 -> MatchSummaryTab(match, s, vm)
            1 -> MatchChatTab(match.id, s, vm)
        }
    }
}

@Composable
fun MatchSummaryTab(match: MatchResponse, s: UiState, vm: AppViewModel) {
    var showResultDialog by remember { mutableStateOf(false) }
    val isPlayed = match.scoreA != null && match.scoreB != null
    val isAdmin = s.currentUserRoleInLeague == "ADMIN" || s.currentUserRoleInLeague == "OWNER"
    val isLeague = s.currentCompetition?.type == "LEAGUE"
    val isCompetitionActive = s.currentCompetition?.active ?: (s.currentCompetition?.status == "ACTIVE" || s.currentCompetition?.status == null)
    
    val canUpdate = !isPlayed && isAdmin
    val canEdit = isPlayed && isLeague && isCompetitionActive && isAdmin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (!match.playedAt.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Giocata il ${DateUtils.formatDate(match.playedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                shape = CircleShape
            ) {
                Text(
                    "DA GIOCARE",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Team A
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = match.teamAName ?: "TBD",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = match.scoreA?.toString() ?: "-",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = if ((match.scoreA ?: 0) > (match.scoreB ?: 0)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    "VS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Team B
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = match.teamBName ?: "TBD",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = match.scoreB?.toString() ?: "-",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = if ((match.scoreB ?: 0) > (match.scoreA ?: 0)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        if (match.cappotto == true) {
            Surface(
                color = MaterialTheme.colorScheme.error,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "CAPPOTTO 🔥",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        }

        if (canUpdate || canEdit) {
            Button(
                onClick = { showResultDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(if (isPlayed) "MODIFICA RISULTATO" else "INSERISCI RISULTATO", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showResultDialog) {
        com.biliardino.ui.components.MatchResultDialog(
            match = match,
            isEdit = isPlayed,
            onDismiss = { showResultDialog = false },
            onConfirm = { sA, sB ->
                val compId = s.currentCompetition?.id ?: 0L
                if (isPlayed) {
                    vm.editMatchResult(compId, match.id, sA, sB)
                } else {
                    vm.updateMatchResult(compId, match.id, sA, sB)
                }
                showResultDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchChatTab(matchId: Long, s: UiState, vm: AppViewModel) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(s.matchComments.size) {
        if (s.matchComments.isNotEmpty()) {
            listState.animateScrollToItem(s.matchComments.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = s.loading,
            onRefresh = { vm.loadMatchComments(matchId) },
            modifier = Modifier.weight(1f)
        ) {
            if (s.matchComments.isEmpty() && !s.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ancora nessun commento. Inizia la conversazione!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val sortedComments = remember(s.matchComments) {
                    s.matchComments.sortedBy { it.createdAt }
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

        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .imePadding(),
                verticalAlignment = Alignment.Bottom
            ) {
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
                            vm.addMatchComment(matchId, messageText)
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

