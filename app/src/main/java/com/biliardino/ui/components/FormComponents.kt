package com.biliardino.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.biliardino.model.LeagueUserResponse
import com.biliardino.model.TeamResponse

@Composable
fun SearchableTeamDropdown(
    teams: List<TeamResponse>,
    selectedTeam: TeamResponse?,
    onTeamSelected: (TeamResponse) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredTeams = remember(searchQuery, teams) {
        if (searchQuery.isBlank()) teams
        else teams.filter { team ->
            team.name.contains(searchQuery, ignoreCase = true) ||
            (team.playerAUsername?.contains(searchQuery, ignoreCase = true) ?: false) ||
            (team.playerBUsername?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    }

    Box(Modifier.widthIn(min = 118.dp, max = 180.dp)) {
        OutlinedButton(
            onClick = { expanded = true; searchQuery = "" },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = selectedTeam?.name ?: "Scegli...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 400.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Cerca squadra o giocatore...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )
            
            if (filteredTeams.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nessun risultato") },
                    onClick = { },
                    enabled = false
                )
            } else {
                filteredTeams.forEach { team ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(team.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "${team.playerAUsername} & ${team.playerBUsername}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        onClick = {
                            onTeamSelected(team)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchablePlayerDropdown(
    users: List<LeagueUserResponse>,
    selectedUser: LeagueUserResponse?,
    onUserSelected: (LeagueUserResponse) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredUsers = remember(searchQuery, users) {
        if (searchQuery.isBlank()) users
        else users.filter { it.username.contains(searchQuery, ignoreCase = true) }
    }

    Box(Modifier.widthIn(min = 118.dp, max = 180.dp)) {
        OutlinedButton(
            onClick = { expanded = true; searchQuery = "" },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedUser?.username ?: "Scegli...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 400.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Cerca giocatore...") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true
            )
            
            if (filteredUsers.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nessun risultato") },
                    onClick = { },
                    enabled = false
                )
            } else {
                filteredUsers.forEach { user ->
                    DropdownMenuItem(
                        text = { Text(user.username) },
                        onClick = {
                            onUserSelected(user)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScorePicker(label: String?, value: Int, onValueChange: (Int) -> Unit) {
    val focusManager = LocalFocusManager.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp)) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
        }
        IconButton(onClick = { onValueChange(value + 1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Add, "Incrementa")
        }
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { onValueChange(it) }
            },
            textStyle = MaterialTheme.typography.displaySmall.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
        IconButton(onClick = { if (value > 0) onValueChange(value - 1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Remove, "Decrementa")
        }
    }
}
