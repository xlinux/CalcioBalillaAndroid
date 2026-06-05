package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.biliardino.ui.components.ProfileHeader
import com.biliardino.ui.components.StatsGrid
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun PlayerProfileScreen(userId: Long, name: String, s: UiState, vm: AppViewModel) {
    val profile = s.currentPlayerProfile

    if (profile == null && s.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (profile == null) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Impossibile caricare il profilo di $name", textAlign = TextAlign.Center)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileHeader(name = profile.username, icon = Icons.Default.Person)
        }

        item {
            StatsGrid(
                stats = listOf(
                    "Partite" to profile.matchesPlayed.toString(),
                    "Vittorie" to profile.wins.toString(),
                    "Pareggi" to profile.draws.toString(),
                    "Sconfitte" to profile.losses.toString(),
                    "Goal Fatti" to profile.goalsFor.toString(),
                    "Goal Subiti" to profile.goalsAgainst.toString(),
                    "Differenza" to profile.goalDifference.toString(),
                    "Win Rate" to "${String.format(java.util.Locale.getDefault(), "%.1f", profile.winPercentage)}%"
                )
            )
        }

        if (profile.trophies.isNotEmpty()) {
            item {
                Text(
                    "🏆 Trofei",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(profile.trophies) { trophy ->
                TrophyCard(trophy, showWinner = false) {
                    // Navigazione futura?
                }
            }
        } else {
            item {
                Text(
                    "🏆 Trofei",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        "Nessun trofeo vinto.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
