package com.biliardino.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import it.gestionecampionati.app.R
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
            if (s.currentScreen !is Screen.Splash && s.currentScreen !is Screen.Onboarding && s.currentScreen !is Screen.MyLeagues) {
                TopAppBar(
                    title = {
                        if (s.currentUser != null || (s.currentScreen !is Screen.PublicLeagues && s.currentScreen !is Screen.AuthMenu)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_logo_old2),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).clip(CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Gestione Campionato Coppe", style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.width(8.dp))
                            }
                        } else {
                            Text(getScreenTitle(s.currentScreen))
                        }
                    },
                navigationIcon = {
                    if (s.currentScreen !is Screen.PublicLeagues) {
                        IconButton(onClick = { 
                            when (val screen = s.currentScreen) {
                                is Screen.AuthMenu -> vm.navigateTo(Screen.PublicLeagues)
                                is Screen.Profile -> vm.navigateTo(Screen.MyLeagues)
                                is Screen.LeagueSeasons -> vm.navigateTo(Screen.MyLeagues)
                                is Screen.SeasonCompetitions -> vm.navigateTo(Screen.LeagueSeasons(screen.league))
                                is Screen.CreateCompetition -> vm.navigateTo(Screen.SeasonCompetitions(screen.league, screen.season))
                                is Screen.CompetitionStatistics -> {
                                    if (s.fromMyCompetitions) {
                                        vm.navigateTo(Screen.MyLeagues)
                                    } else {
                                        vm.navigateTo(Screen.SeasonCompetitions(screen.league, screen.season))
                                    }
                                }
                                is Screen.CompetitionMatches -> {
                                    if (s.fromMyCompetitions) {
                                        vm.navigateTo(Screen.MyLeagues)
                                    } else {
                                        vm.navigateTo(Screen.SeasonCompetitions(screen.league, screen.season))
                                    }
                                }
                                is Screen.CompetitionParticipants -> {
                                    if (s.fromMyCompetitions) {
                                        vm.navigateTo(Screen.MyLeagues)
                                    } else {
                                        vm.navigateTo(Screen.SeasonSettings(screen.league, screen.season, screen.competition))
                                    }
                                }
                                is Screen.SeasonTeams -> {
                                    if (screen.competition != null) {
                                        if (s.fromMyCompetitions) {
                                            vm.navigateTo(Screen.MyLeagues)
                                        } else {
                                            vm.navigateTo(Screen.CompetitionStatistics(screen.league, screen.season, screen.competition))
                                        }
                                    } else {
                                        vm.navigateTo(Screen.SeasonCompetitions(screen.league, screen.season))
                                    }
                                }
                                is Screen.SeasonSettings -> vm.navigateTo(Screen.SeasonCompetitions(screen.league, screen.season))
                                is Screen.TeamDetail -> vm.navigateTo(Screen.CompetitionStatistics(screen.league, screen.season, screen.competition))
                                is Screen.PlayerDetail -> vm.navigateTo(Screen.CompetitionStatistics(screen.league, season = screen.season, competition = screen.competition))
                                is Screen.PlayerProfile -> {
                                    val league = s.currentLeague
                                    val season = s.currentSeason
                                    val competition = s.currentCompetition
                                    if (league != null && season != null && competition != null) {
                                        vm.navigateTo(Screen.CompetitionStatistics(league, season, competition))
                                    } else {
                                        vm.navigateTo(Screen.MyLeagues)
                                    }
                                }
                                is Screen.TeamProfile -> {
                                    val league = s.currentLeague
                                    val season = s.currentSeason
                                    val competition = s.currentCompetition
                                    if (league != null && season != null && competition != null) {
                                        vm.navigateTo(Screen.CompetitionStatistics(league, season, competition))
                                    } else {
                                        vm.navigateTo(Screen.MyLeagues)
                                    }
                                }
                                is Screen.MatchDetail -> {
                                    val league = s.currentLeague
                                    val season = s.currentSeason
                                    val competition = s.currentCompetition
                                    if (league != null && season != null && competition != null) {
                                        val phase = competition.phase
                                        if (phase == "FINAL_STAGE" || phase == "READY_FOR_FINAL_STAGE") {
                                            vm.updateCompetitionMode("MAIN")
                                            val isGroups = competition.tournamentFormat == "GROUPS_THEN_SINGLE_ELIMINATION"
                                            if (competition.type == "CUP" || !isGroups) {
                                                vm.updateCompetitionTab(0) // Tabellone
                                            } else {
                                                vm.updateCompetitionTab(1) // Fase Finale
                                            }
                                        } else {
                                            vm.updateCompetitionMode("MATCHES") // Return to "Partite" area
                                        }
                                        vm.navigateTo(Screen.CompetitionStatistics(league, season, competition))
                                    } else {
                                        vm.navigateTo(Screen.MyLeagues)
                                    }
                                }
                                is Screen.JoinTeam -> vm.navigateTo(Screen.SeasonCompetitions(screen.league, screen.season))
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
                            TextButton(onClick = vm::logout) { Text("Logout") }
                        }
                    } else if (s.currentScreen is Screen.PublicLeagues) {
                        TextButton(onClick = { vm.navigateTo(Screen.AuthMenu) }) { Text("Accedi") }
                    }
                }
            )
        }
    },
        bottomBar = {
            val screen = s.currentScreen
            if (screen is Screen.CompetitionStatistics || screen is Screen.CompetitionMatches || screen is Screen.CompetitionParticipants || screen is Screen.SeasonTeams || screen is Screen.SeasonSettings || screen is Screen.TeamDetail || screen is Screen.PlayerDetail) {
                val league = when (screen) {
                    is Screen.CompetitionStatistics -> screen.league
                    is Screen.CompetitionMatches -> screen.league
                    is Screen.CompetitionParticipants -> screen.league
                    is Screen.SeasonTeams -> screen.league
                    is Screen.SeasonSettings -> screen.league
                    is Screen.TeamDetail -> screen.league
                    is Screen.PlayerDetail -> screen.league
                    else -> null
                }
                val season = when (screen) {
                    is Screen.CompetitionStatistics -> screen.season
                    is Screen.CompetitionMatches -> screen.season
                    is Screen.CompetitionParticipants -> screen.season
                    is Screen.SeasonTeams -> screen.season
                    is Screen.SeasonSettings -> screen.season
                    is Screen.TeamDetail -> screen.season
                    is Screen.PlayerDetail -> screen.season
                    else -> null
                }
                val competition = when (screen) {
                    is Screen.CompetitionStatistics -> screen.competition
                    is Screen.CompetitionMatches -> screen.competition
                    is Screen.CompetitionParticipants -> screen.competition
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
                                selected = s.currentCompetitionMode == "MAIN" && screen is Screen.CompetitionStatistics,
                                onClick = { 
                                    vm.updateCompetitionMode("MAIN")
                                    vm.navigateTo(Screen.CompetitionStatistics(league, season, competition))
                                    vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
                                },
                                icon = { Icon(Icons.Default.List, contentDescription = "Home") },
                                label = { Text(if (competition.type == "CUP") "Tabellone" else "Home") }
                            )
                            
                            NavigationBarItem(
                                selected = s.currentCompetitionMode == "MATCHES",
                                onClick = {
                                    vm.updateCompetitionMode("MATCHES")
                                    vm.navigateTo(Screen.CompetitionStatistics(league, season, competition))
                                    vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
                                },
                                icon = { Icon(Icons.Default.Add, contentDescription = "Partite") },
                                label = { Text("Partite") }
                            )

                            // "Storico" tab for all, positioned after Partite
                            NavigationBarItem(
                                selected = s.currentCompetitionMode == "HISTORY",
                                onClick = { 
                                    vm.updateCompetitionMode("HISTORY")
                                    vm.navigateTo(Screen.CompetitionStatistics(league, season, competition))
                                    vm.refreshCompetitionData(league.id, competition.id, competition.rankingMode)
                                },
                                icon = { Icon(Icons.Default.History, contentDescription = "Storico") },
                                label = { Text("Storico") }
                            )
                        }

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
                    is Screen.Onboarding -> OnboardingScreen(vm)
                    is Screen.PublicLeagues -> PublicLeaguesScreen(s, vm)
                    is Screen.AuthMenu -> AuthScreen(vm)
                    is Screen.MyLeagues -> MyLeaguesScreen(s, vm)
                    is Screen.Profile -> ProfileScreen(s, vm)
                    is Screen.LeagueSeasons -> LeagueSeasonsScreen(screen.league, s, vm)
                    is Screen.SeasonCompetitions -> SeasonCompetitionsScreen(screen.league, screen.season, s, vm)
                    is Screen.CreateCompetition -> CreateCompetitionScreen(screen.league, screen.season, s, vm)
                    is Screen.CompetitionStatistics -> CompetitionStatisticsScreen(screen.league, screen.season, screen.competition, s, vm)
                    is Screen.CompetitionMatches -> CompetitionMatchesScreen(screen.league, screen.season, screen.competition, s, vm)
                    is Screen.CompetitionParticipants -> CompetitionParticipantsScreen(screen.league, screen.season, screen.competition, s, vm)
                    is Screen.SeasonTeams -> SeasonTeamsScreen(screen.league, screen.season, screen.competition, s, vm)
                    is Screen.SeasonSettings -> SeasonSettingsScreen(screen.league, screen.season, screen.competition, s, vm)
                    is Screen.TeamDetail -> TeamDetailScreen(screen.league, screen.season, screen.competition, screen.team, s, vm)
                    is Screen.PlayerDetail -> PlayerDetailScreen(screen.league, screen.season, screen.competition, screen.user, s, vm)
                    is Screen.PlayerProfile -> PlayerProfileScreen(screen.userId, screen.name, s, vm)
                    is Screen.TeamProfile -> TeamProfileScreen(screen.teamId, screen.name, s, vm)
                    is Screen.MatchDetail -> MatchDetailScreen(screen.match, s, vm)
                    is Screen.JoinTeam -> JoinTeamScreen(screen.league, screen.season, screen.competition, s, vm)
                    is Screen.CreateTeam -> CreateTeamScreen(screen.league, screen.season, screen.competition, s, vm)
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
    is Screen.Splash -> "CampionatoCoppe"
    is Screen.Onboarding -> ""
    is Screen.PublicLeagues -> "Leghe Pubbliche"
    is Screen.AuthMenu -> "Benvenuto"
    is Screen.MyLeagues -> "Le mie Leghe"
    is Screen.Profile -> "Profilo"
    is Screen.LeagueSeasons -> screen.league.name
    is Screen.SeasonCompetitions -> screen.season.name
    is Screen.CreateCompetition -> "Nuova Competizione"
    is Screen.CompetitionStatistics -> screen.competition.name
    is Screen.CompetitionMatches -> "Partite"
    is Screen.CompetitionParticipants -> "Partecipanti"
    is Screen.SeasonTeams -> "Squadre"
    is Screen.SeasonSettings -> "Impostazioni"
    is Screen.TeamDetail -> screen.team.name
    is Screen.PlayerDetail -> screen.user.username
    is Screen.PlayerProfile -> "Profilo: ${screen.name}"
    is Screen.TeamProfile -> "Profilo: ${screen.name}"
    is Screen.MatchDetail -> "Dettaglio Partita"
    is Screen.JoinTeam -> "Partecipa alla Squadra"
    is Screen.CreateTeam -> "Nuova Squadra"
}
