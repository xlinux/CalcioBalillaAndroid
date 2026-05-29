package com.biliardino.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.biliardino.R
import com.biliardino.ui.screens.*
import com.biliardino.ui.components.*
import com.biliardino.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentScreen(vm: AppViewModel) {
    val s by vm.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Reazione ai messaggi
    LaunchedEffect(s.error, s.successMessage) {
        s.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            vm.clearMessages()
        }
        s.successMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
            vm.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (s.currentScreen !is Screen.Splash) {
                TopAppBar(
                    title = {
                        if (s.currentUser != null || (s.currentScreen !is Screen.PublicLeagues && s.currentScreen !is Screen.AuthMenu)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_logo),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).clip(CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Leghe Calcio Balilla", style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.width(8.dp))
                            }
                        } else {
                            Text(getScreenTitle(s.currentScreen))
                        }
                    },
                navigationIcon = {
                    if (s.currentScreen !is Screen.PublicLeagues && s.currentScreen !is Screen.MyLeagues) {
                        IconButton(onClick = { 
                            when (val screen = s.currentScreen) {
                                is Screen.AuthMenu -> vm.navigateTo(Screen.PublicLeagues)
                                is Screen.Profile -> vm.navigateTo(Screen.MyLeagues)
                                is Screen.LeagueSeasons -> vm.navigateTo(Screen.MyLeagues)
                                is Screen.SeasonCompetitions -> vm.navigateTo(Screen.LeagueSeasons(screen.league))
                                is Screen.CompetitionStatistics -> vm.navigateTo(Screen.SeasonCompetitions(screen.league, screen.season))
                                is Screen.CompetitionMatches -> vm.navigateTo(Screen.SeasonCompetitions(screen.league, screen.season))
                                is Screen.SeasonTeams -> vm.navigateTo(Screen.LeagueSeasons(screen.league))
                                is Screen.SeasonSettings -> vm.navigateTo(Screen.LeagueSeasons(screen.league))
                                is Screen.TeamDetail -> vm.navigateTo(Screen.CompetitionStatistics(screen.league, screen.season, screen.competition))
                                is Screen.PlayerDetail -> vm.navigateTo(Screen.CompetitionStatistics(screen.league, screen.season, screen.competition))
                                else -> {}
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                        }
                    }
                },
                actions = {
                    if (s.currentUser != null || (s.currentScreen !is Screen.PublicLeagues && s.currentScreen !is Screen.AuthMenu)) {
                        if (s.currentScreen !is Screen.Profile) {
                            IconButton(onClick = { 
                                vm.loadCurrentUser()
                                vm.navigateTo(Screen.Profile) 
                            }) {
                                Icon(Icons.Default.Person, contentDescription = "Profilo")
                            }
                        }
                        TextButton(onClick = vm::logout) { Text("Logout") }
                    } else if (s.currentScreen is Screen.PublicLeagues) {
                        TextButton(onClick = { vm.navigateTo(Screen.AuthMenu) }) { Text("Accedi") }
                    }
                }
            )
        }
    },
        bottomBar = {
            val screen = s.currentScreen
            if (screen is Screen.CompetitionStatistics || screen is Screen.CompetitionMatches || screen is Screen.SeasonTeams || screen is Screen.SeasonSettings || screen is Screen.TeamDetail || screen is Screen.PlayerDetail) {
                val league = when (screen) {
                    is Screen.CompetitionStatistics -> screen.league
                    is Screen.CompetitionMatches -> screen.league
                    is Screen.SeasonTeams -> screen.league
                    is Screen.SeasonSettings -> screen.league
                    is Screen.TeamDetail -> screen.league
                    is Screen.PlayerDetail -> screen.league
                    else -> null
                }
                val season = when (screen) {
                    is Screen.CompetitionStatistics -> screen.season
                    is Screen.CompetitionMatches -> screen.season
                    is Screen.SeasonTeams -> screen.season
                    is Screen.SeasonSettings -> screen.season
                    is Screen.TeamDetail -> screen.season
                    is Screen.PlayerDetail -> screen.season
                    else -> null
                }
                val competition = when (screen) {
                    is Screen.CompetitionStatistics -> screen.competition
                    is Screen.CompetitionMatches -> screen.competition
                    is Screen.SeasonTeams -> screen.competition
                    is Screen.SeasonSettings -> screen.competition
                    is Screen.TeamDetail -> screen.competition
                    is Screen.PlayerDetail -> screen.competition
                    else -> null
                }

                if (league != null && season != null) {
                    NavigationBar {
                        if (competition != null) {
                            NavigationBarItem(
                                selected = screen is Screen.CompetitionStatistics || screen is Screen.TeamDetail || screen is Screen.PlayerDetail,
                                onClick = { 
                                    vm.navigateTo(Screen.CompetitionStatistics(league, season, competition))
                                    vm.loadRankings(competition.id, competition.rankingMode)
                                },
                                icon = { Icon(Icons.Default.List, contentDescription = "Home") },
                                label = { Text("Stats") }
                            )
                            NavigationBarItem(
                                selected = screen is Screen.CompetitionMatches,
                                onClick = { 
                                    vm.navigateTo(Screen.CompetitionMatches(league, season, competition))
                                    vm.loadCompetitionMatches(competition.id)
                                },
                                icon = { Icon(Icons.Default.Add, contentDescription = "Partite") },
                                label = { Text("Partite") }
                            )
                        }
                        NavigationBarItem(
                            selected = screen is Screen.SeasonTeams,
                            onClick = { 
                                vm.navigateTo(Screen.SeasonTeams(league, season, competition))
                                vm.loadSeasonStatsData(league.id, season.id)
                            },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Squadre") },
                            label = { Text("Squadre") }
                        )
                        NavigationBarItem(
                            selected = screen is Screen.SeasonSettings,
                            onClick = { 
                                vm.navigateTo(Screen.SeasonSettings(league, season, competition))
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Impostazioni") },
                            label = { Text("Impostazioni") }
                        )
                    }
                }
            }
        }
    ) { padding ->
        val modifier = if (s.currentScreen is Screen.Splash) {
            Modifier.fillMaxSize()
        } else {
            Modifier.padding(padding).fillMaxSize()
        }
        Box(modifier = modifier) {
            Crossfade(targetState = s.currentScreen) { screen ->
                when (screen) {
                    is Screen.Splash -> SplashScreen()
                    is Screen.PublicLeagues -> PublicLeaguesScreen(s, vm)
                    is Screen.AuthMenu -> AuthScreen(s, vm)
                    is Screen.MyLeagues -> MyLeaguesScreen(s, vm)
                    is Screen.Profile -> ProfileScreen(s, vm)
                    is Screen.LeagueSeasons -> LeagueSeasonsScreen(screen.league, s, vm)
                    is Screen.SeasonCompetitions -> SeasonCompetitionsScreen(screen.league, screen.season, s, vm)
                    is Screen.CompetitionStatistics -> CompetitionStatisticsScreen(screen.league, screen.season, screen.competition, s, vm)
                    is Screen.CompetitionMatches -> CompetitionMatchesScreen(screen.league, screen.season, screen.competition, s, vm)
                    is Screen.SeasonTeams -> SeasonTeamsScreen(screen.league, screen.season, screen.competition, s, vm)
                    is Screen.SeasonSettings -> SeasonSettingsScreen(screen.league, screen.season, screen.competition, s, vm)
                    is Screen.TeamDetail -> TeamDetailScreen(screen.league, screen.season, screen.competition, screen.team, s, vm)
                    is Screen.PlayerDetail -> PlayerDetailScreen(screen.league, screen.season, screen.competition, screen.user, s, vm)
                    else -> {}
                }
            }

            if (s.showBiometricSetupPrompt) {
                AlertDialog(
                    onDismissRequest = vm::dismissBiometricPrompt,
                    title = { Text("Attiva Biometria") },
                    text = { Text("Vuoi attivare l'accesso rapido con Touch ID / Biometria per i prossimi login?") },
                    confirmButton = {
                        Button(onClick = { vm.enableBiometric(true) }) { Text("Attiva") }
                    },
                    dismissButton = {
                        TextButton(onClick = { vm.enableBiometric(false) }) { Text("Non ora") }
                    }
                )
            }

            if (s.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

private fun getScreenTitle(screen: Screen): String = when (screen) {
    is Screen.Splash -> "Biliardino"
    is Screen.PublicLeagues -> "Leghe Pubbliche"
    is Screen.AuthMenu -> "Benvenuto"
    is Screen.MyLeagues -> "Le mie Leghe"
    is Screen.Profile -> "Profilo"
    is Screen.LeagueSeasons -> screen.league.name
    is Screen.SeasonCompetitions -> screen.season.name
    is Screen.CompetitionStatistics -> screen.competition.name
    is Screen.CompetitionMatches -> "Partite"
    is Screen.SeasonTeams -> "Squadre"
    is Screen.SeasonSettings -> "Impostazioni"
    is Screen.TeamDetail -> screen.team.name
    is Screen.PlayerDetail -> screen.user.username
}
