package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

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

@Composable
fun rankAccent(rank: Int): Color = when (rank) {
    1 -> Color(0xFFFFD700)
    2 -> Color(0xFFC0C0C0)
    3 -> Color(0xFFCD7F32)
    else -> MaterialTheme.colorScheme.primary
}
