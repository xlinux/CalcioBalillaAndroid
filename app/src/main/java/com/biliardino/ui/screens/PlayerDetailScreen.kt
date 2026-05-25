package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.biliardino.model.*
import com.biliardino.ui.components.StatRow
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun PlayerDetailScreen(league: LeagueResponse, season: SeasonResponse, user: LeagueUserResponse, s: UiState, vm: AppViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PlayerStatsInfo(s.currentPlayerStats)
        
        if (s.currentPlayerPartners.isNotEmpty()) {
            PlayerPartnersInfo(s.currentPlayerPartners)
        }

        if (s.currentPlayerRatingHistory.isNotEmpty()) {
            Text("Andamento Rating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            RatingLineChart(s.currentPlayerRatingHistory, Modifier.fillMaxWidth().height(200.dp))
        }
    }
}

@Composable
fun PlayerPartnersInfo(partners: List<PlayerPartnerStatsResponse>) {
    val bestPartner = partners.maxByOrNull { it.winPercentage }
    val worstPartner = partners.minByOrNull { it.winPercentage }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Compagni di Squadra", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bestPartner?.let {
                PartnerHighlightCard("Migliore", it, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
            }
            worstPartner?.let {
                PartnerHighlightCard("Peggiore", it, MaterialTheme.colorScheme.errorContainer, Modifier.weight(1f))
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Tutti i Compagni", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                partners.sortedByDescending { it.matchesPlayed }.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.partnerUsername, fontWeight = FontWeight.Bold)
                            Text("${p.matchesPlayed} partite", style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${"%.0f".format(p.winPercentage)}% Win", fontWeight = FontWeight.Medium)
                            Text(
                                "${if (p.ratingDelta >= 0) "+" else ""}${p.ratingDelta} ELO",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (p.ratingDelta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (p != partners.last()) HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun PartnerHighlightCard(label: String, partner: PlayerPartnerStatsResponse, containerColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(partner.partnerUsername, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${"%.0f".format(partner.winPercentage)}% Win", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun PlayerStatsInfo(stats: PlayerStatsResponse?) {
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
