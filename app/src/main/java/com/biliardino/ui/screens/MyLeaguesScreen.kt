package com.biliardino.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.biliardino.viewmodel.AppViewModel
import com.biliardino.viewmodel.UiState

@Composable
fun MyLeaguesScreen(s: UiState, vm: AppViewModel) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(s.myLeagues) { league ->
                LeagueCard(
                    league = league,
                    onCLick = { vm.selectLeague(league) },
                    showInviteCode = true
                )
            }
        }
        FloatingActionButton(
            onClick = { /* TODO: Create League */ },
            modifier = Modifier.padding(16.dp).align(Alignment.End)
        ) { Icon(Icons.Default.Add, "Crea Lega") }
    }
}
