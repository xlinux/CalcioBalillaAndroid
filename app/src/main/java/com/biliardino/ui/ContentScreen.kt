package com.biliardino.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        s.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            vm.clearMessages()
        }
        s.successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
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
                    title = { Text(getScreenTitle(s.currentScreen)) },
                navigationIcon = {
                    if (s.currentScreen !is Screen.PublicLeagues && s.currentScreen !is Screen.MyLeagues) {
                        IconButton(onClick = { 
                            when (val screen = s.currentScreen) {
                                is Screen.AuthMenu -> vm.navigateTo(Screen.PublicLeagues)
                                is Screen.LeagueSeasons -> vm.navigateTo(Screen.MyLeagues)
                                is Screen.SeasonDetail -> vm.navigateTo(Screen.LeagueSeasons(screen.league))
                                is Screen.SeasonStatistics -> vm.navigateTo(Screen.LeagueSeasons(screen.league))
                                is Screen.SeasonMatches -> vm.navigateTo(Screen.LeagueSeasons(screen.league))
                                is Screen.SeasonTeams -> vm.navigateTo(Screen.LeagueSeasons(screen.league))
                                is Screen.TeamDetail -> vm.navigateTo(Screen.SeasonStatistics(screen.league, screen.season))
                                is Screen.PlayerDetail -> vm.navigateTo(Screen.SeasonStatistics(screen.league, screen.season))
                                else -> {}
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                        }
                    }
                },
                actions = {
                    if (s.currentUser != null || s.currentScreen !is Screen.PublicLeagues && s.currentScreen !is Screen.AuthMenu) {
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
            if (screen is Screen.SeasonDetail || screen is Screen.SeasonStatistics || screen is Screen.SeasonMatches || screen is Screen.SeasonTeams || screen is Screen.TeamDetail || screen is Screen.PlayerDetail) {
                val league = when (screen) {
                    is Screen.SeasonDetail -> screen.league
                    is Screen.SeasonStatistics -> screen.league
                    is Screen.SeasonMatches -> screen.league
                    is Screen.SeasonTeams -> screen.league
                    is Screen.TeamDetail -> screen.league
                    is Screen.PlayerDetail -> screen.league
                    else -> null
                }
                val season = when (screen) {
                    is Screen.SeasonDetail -> screen.season
                    is Screen.SeasonStatistics -> screen.season
                    is Screen.SeasonMatches -> screen.season
                    is Screen.SeasonTeams -> screen.season
                    is Screen.TeamDetail -> screen.season
                    is Screen.PlayerDetail -> screen.season
                    else -> null
                }

                if (league != null && season != null) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = screen is Screen.SeasonStatistics || screen is Screen.TeamDetail || screen is Screen.PlayerDetail,
                            onClick = { 
                                vm.navigateTo(Screen.SeasonStatistics(league, season))
                                vm.loadSeasonStatsData(league.id, season.id)
                            },
                            icon = { Icon(Icons.Default.List, contentDescription = "Home") },
                            label = { Text("Stats") }
                        )
                        NavigationBarItem(
                            selected = screen is Screen.SeasonMatches,
                            onClick = { 
                                vm.navigateTo(Screen.SeasonMatches(league, season))
                                vm.loadSeasonStatsData(league.id, season.id)
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = "Partite") },
                            label = { Text("Partite") }
                        )
                        NavigationBarItem(
                            selected = screen is Screen.SeasonTeams,
                            onClick = { 
                                vm.navigateTo(Screen.SeasonTeams(league, season))
                                vm.loadSeasonStatsData(league.id, season.id)
                            },
                            icon = { Icon(Icons.Default.Person, contentDescription = "Squadre") },
                            label = { Text("Squadre") }
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
                    is Screen.LeagueSeasons -> LeagueSeasonsScreen(screen.league, s, vm)
                    is Screen.SeasonDetail, is Screen.SeasonStatistics -> {
                        val l = if (screen is Screen.SeasonDetail) screen.league else (screen as Screen.SeasonStatistics).league
                        val sea = if (screen is Screen.SeasonDetail) screen.season else (screen as Screen.SeasonStatistics).season
                        SeasonStatisticsScreen(l, sea, s, vm)
                    }
                    is Screen.SeasonMatches -> SeasonMatchesScreen(screen.league, screen.season, s, vm)
                    is Screen.SeasonTeams -> SeasonTeamsScreen(screen.league, screen.season, s, vm)
                    is Screen.TeamDetail -> TeamDetailScreen(screen.league, screen.season, screen.team, s, vm)
                    is Screen.PlayerDetail -> PlayerDetailScreen(screen.league, screen.season, screen.user, s, vm)
                }
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
    is Screen.LeagueSeasons -> screen.league.name
    is Screen.SeasonDetail -> screen.season.name
    is Screen.SeasonStatistics -> "Statistiche"
    is Screen.SeasonMatches -> "Partite"
    is Screen.SeasonTeams -> "Squadre"
    is Screen.TeamDetail -> screen.team.name
    is Screen.PlayerDetail -> screen.user.username
}
