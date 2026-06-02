package com.biliardino.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biliardino.model.*
import com.biliardino.network.ApiClientBase
import com.biliardino.network.SessionManager
import com.biliardino.ui.Screen
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.InputStream

data class UiState(
    val currentUser: AuthResponse? = null,
    val currentScreen: Screen = Screen.Splash,
    val publicLeagues: List<LeagueResponse> = emptyList(),
    val myLeagues: List<LeagueResponse> = emptyList(),
    val currentLeague: LeagueResponse? = null,
    val seasons: List<SeasonResponse> = emptyList(),
    val currentSeason: SeasonResponse? = null,
    val competitions: List<CompetitionResponse> = emptyList(),
    val sports: List<SportResponse> = emptyList(),
    val competitionTemplates: List<CompetitionTemplateResponse> = emptyList(),
    val currentCompetition: CompetitionResponse? = null,
    val playerRankings: List<PlayerRankingResponse> = emptyList(),
    val teamRankings: List<TeamRankingResponse> = emptyList(),
    val competitionPlayers: List<LeagueMemberResponse> = emptyList(),
    val teamMembers: List<TeamMemberResponse> = emptyList(),
    val seasonMatches: List<MatchResponse> = emptyList(),
    val seasonTeams: List<TeamResponse> = emptyList(),
    val seasonUsers: List<LeagueUserResponse> = emptyList(),
    val leagueMembers: List<LeagueMemberResponse> = emptyList(),
    val currentTeamStats: TeamStatsResponse? = null,
    val currentTeamRatingHistory: List<RatingHistoryResponse> = emptyList(),
    val currentTeamMatches: List<MatchResponse> = emptyList(),
    val currentTeamHeadToHead: HeadToHeadResponse? = null,
    val currentPlayerStats: PlayerStatsResponse? = null,
    val currentPlayerRatingHistory: List<RatingHistoryResponse> = emptyList(),
    val currentPlayerPartners: List<PlayerPartnerStatsResponse> = emptyList(),
    val currentPlayerMatches: List<MatchResponse> = emptyList(),
    val currentUserRoleInLeague: String? = null,
    
    // Create League fields
    val newLeagueName: String = "",
    val newLeagueDescription: String = "",
    val inviteCode: String = "",
    
    // Auth fields
    val email: String = "",
    val password: String = "",
    val username: String = "",
    
    val loading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val canUseBiometrics: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val showBiometricSetupPrompt: Boolean = false,
    val theme: String = "SYSTEM" // "LIGHT", "DARK", "SYSTEM"
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private val sessionManager = SessionManager(application)

    init {
        ApiClientBase.init(application)
        ApiClientBase.onAuthFailure = {
            logout()
        }
        checkBiometricAvailability()
        viewModelScope.launch {
            sessionManager.isBiometricEnabled.collect { enabled ->
                _state.value = _state.value.copy(isBiometricEnabled = enabled)
            }
        }
        viewModelScope.launch {
            sessionManager.themePreference.collect { theme ->
                _state.value = _state.value.copy(theme = theme)
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            checkSession()
            loadPublicLeagues()
        }
    }

    private fun checkSession() = viewModelScope.launch {
        val token = sessionManager.authToken.first()
        val refreshToken = sessionManager.refreshToken.first()
        if (token != null && refreshToken != null) {
            ApiClientBase.authToken = token
            loadCurrentUser()
            _state.value = _state.value.copy(currentScreen = Screen.MyLeagues)
            loadMyLeagues()
        } else {
            _state.value = _state.value.copy(currentScreen = Screen.PublicLeagues)
        }
    }

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(getApplication())
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        _state.value = _state.value.copy(
            canUseBiometrics = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
        )
    }

    fun onEmailChange(v: String) { _state.value = _state.value.copy(email = v) }
    fun onPasswordChange(v: String) { _state.value = _state.value.copy(password = v) }
    fun onUsernameChange(v: String) { _state.value = _state.value.copy(username = v) }

    fun onNewLeagueNameChange(v: String) { _state.value = _state.value.copy(newLeagueName = v) }
    fun onNewLeagueDescriptionChange(v: String) { _state.value = _state.value.copy(newLeagueDescription = v) }
    fun onInviteCodeChange(v: String) { _state.value = _state.value.copy(inviteCode = v) }

    fun navigateTo(screen: Screen) {
        _state.value = _state.value.copy(currentScreen = screen, error = null, successMessage = null)
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, successMessage = null)
    }

    fun login() = viewModelScope.launch {
        val s = _state.value
        _state.value = s.copy(loading = true, error = null, successMessage = null)
        runCatching {
            ApiClientBase.auth.login(LoginRequest(s.email, s.password))
        }.onSuccess { auth ->
            val jwt = auth.token ?: auth.jwt
            
            if (jwt != null) {
                ApiClientBase.authToken = jwt
                // Salviamo lo stesso token anche come refresh se il backend non ne manda uno specifico
                val refreshToken = auth.refreshToken ?: jwt
                sessionManager.saveTokens(jwt, refreshToken)
                
                // Fetch complete user info after login
                loadCurrentUser()

                val showBioPrompt = s.canUseBiometrics && !s.isBiometricEnabled

                _state.value = _state.value.copy(
                    currentUser = auth.copy(token = jwt, refreshToken = refreshToken),
                    loading = false,
                    currentScreen = Screen.MyLeagues,
                    successMessage = "Login effettuato con successo!",
                    showBiometricSetupPrompt = showBioPrompt
                )
                loadMyLeagues()
            } else {
                _state.value = _state.value.copy(loading = false, error = "Errore: token mancante")
            }
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Login fallito: ${e.getErrorMessage()}")
        }
    }

    fun register() = viewModelScope.launch {
        val s = _state.value
        _state.value = s.copy(loading = true, error = null, successMessage = null)
        runCatching {
            ApiClientBase.auth.register(RegisterRequest(s.username, s.email, s.password))
        }.onSuccess { auth ->
            val jwt = auth.jwt
            val refreshToken = auth.refreshToken
            
            if (jwt != null && refreshToken != null) {
                ApiClientBase.authToken = jwt
                sessionManager.saveTokens(jwt, refreshToken)
                
                val showBioPrompt = s.canUseBiometrics && !s.isBiometricEnabled

                _state.value = _state.value.copy(
                    currentUser = auth,
                    loading = false,
                    currentScreen = Screen.MyLeagues,
                    successMessage = "Registrazione completata!",
                    showBiometricSetupPrompt = showBioPrompt
                )
                loadMyLeagues()
            } else {
                _state.value = _state.value.copy(loading = false, error = "Errore: token mancante")
            }
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Registrazione fallita: ${e.getErrorMessage()}")
        }
    }

    fun enableBiometric(enable: Boolean) = viewModelScope.launch {
        sessionManager.setBiometricEnabled(enable)
        _state.value = _state.value.copy(showBiometricSetupPrompt = false)
        if (enable) {
            _state.value = _state.value.copy(successMessage = "Touch ID attivato!")
        }
    }

    fun setTheme(theme: String) = viewModelScope.launch {
        sessionManager.setThemePreference(theme)
    }

    fun dismissBiometricPrompt() {
        _state.value = _state.value.copy(showBiometricSetupPrompt = false)
    }

    fun loadPublicLeagues() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.getPublicLeagues() }
            .onSuccess { 
                _state.value = _state.value.copy(
                    publicLeagues = it, 
                    loading = false
                ) 
            }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.getErrorMessage()) }
    }

    fun loadSports() = viewModelScope.launch {
        Log.d("AppViewModel", "Loading sports...")
        runCatching { ApiClientBase.sports.getSports() }
            .onSuccess {
                Log.d("AppViewModel", "Sports loaded: ${it.size}")
                _state.value = _state.value.copy(sports = it)
            }
            .onFailure { e ->
                Log.e("AppViewModel", "Failed to load sports", e)
                _state.value = _state.value.copy(error = "Errore nel caricamento degli sport: ${e.getErrorMessage()}")
            }
    }

    fun loadCompetitionTemplates() = viewModelScope.launch {
        Log.d("AppViewModel", "Loading competition templates...")
        runCatching { ApiClientBase.sports.getCompetitionTemplates() }
            .onSuccess {
                Log.d("AppViewModel", "Templates loaded: ${it.size}")
                _state.value = _state.value.copy(competitionTemplates = it)
            }
            .onFailure { e ->
                Log.e("AppViewModel", "Failed to load templates", e)
                _state.value = _state.value.copy(error = "Errore nel caricamento dei template: ${e.getErrorMessage()}")
            }
    }

    fun loadMyLeagues() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.getMyLeagues() }
            .onSuccess { 
                _state.value = _state.value.copy(
                    myLeagues = it, 
                    loading = false
                ) 
            }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.getErrorMessage()) }
    }

    fun selectLeague(league: LeagueResponse) = viewModelScope.launch {
        _state.value = _state.value.copy(currentLeague = league, loading = true, error = null, successMessage = null, currentUserRoleInLeague = null)
        runCatching {
            val seasons = ApiClientBase.leagues.getSeasons(league.id)
            val users = ApiClientBase.leagues.getLeagueUsers(league.id)
            val members = ApiClientBase.leagues.getLeagueMembers(league.id)
            Triple(seasons, users, members)
        }.onSuccess { (seasons, users, members) ->
            val myUserId = _state.value.currentUser?.userId
            val myRole = members.find { it.userId == myUserId }?.role
            
            _state.value = _state.value.copy(
                seasons = seasons,
                seasonUsers = users,
                leagueMembers = members,
                currentUserRoleInLeague = myRole,
                loading = false,
                currentScreen = Screen.LeagueSeasons(league)
            ) 
        }
        .onFailure { _state.value = _state.value.copy(loading = false, error = it.getErrorMessage()) }
    }

    fun selectSeason(league: LeagueResponse, season: SeasonResponse) = viewModelScope.launch {
        _state.value = _state.value.copy(
            currentSeason = season,
            loading = true,
            error = null
        )
        runCatching { ApiClientBase.leagues.getCompetitions(season.id) }
            .onSuccess { competitions ->
                _state.value = _state.value.copy(
                    competitions = competitions,
                    loading = false,
                    currentScreen = Screen.SeasonCompetitions(league, season)
                )
                loadSeasonStatsData(league.id, season.id)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore competizioni: ${e.getErrorMessage()}")
            }
    }

    fun selectCompetition(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse) {
        _state.value = _state.value.copy(
            currentLeague = league,
            currentSeason = season,
            currentCompetition = competition,
            currentScreen = Screen.CompetitionStatistics(league, season, competition)
        )
    }

    fun refreshCompetitionData(leagueId: Long, competitionId: Long, rankingMode: String? = null) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        val resolvedRankingMode = rankingMode ?: rankingModeForCompetition(competitionId)
        
        runCatching {
            // 1. Carichiamo Classifiche
            val playersRank = if (resolvedRankingMode != "TEAM") ApiClientBase.competitions.getPlayerRankings(competitionId) else emptyList()
            val teamsRank = if (resolvedRankingMode != "PLAYER") ApiClientBase.competitions.getTeamRankings(competitionId) else emptyList()

            // 2. Carichiamo Match
            val matches = ApiClientBase.competitions.getMatches(competitionId)

            // 3. Carichiamo Squadre
            val teams = ApiClientBase.competitions.getCompetitionTeams(competitionId)

            // 4. Carichiamo Partecipanti (e dati lega per arricchirli)
            val compPlayers = ApiClientBase.competitions.getCompetitionPlayers(competitionId)
            val leagueUsers = ApiClientBase.leagues.getLeagueUsers(leagueId)

            val enrichedUsers = compPlayers.map { player ->
                val lu = leagueUsers.find { it.userId == player.userId }
                LeagueUserResponse(
                    userId = player.userId,
                    username = player.username,
                    email = player.email,
                    rating = lu?.rating ?: 0,
                    goalsFor = lu?.goalsFor ?: 0,
                    goalsAgainst = lu?.goalsAgainst ?: 0,
                    matchesPlayed = lu?.matchesPlayed ?: 0,
                    cappottiGiven = lu?.cappottiGiven ?: 0,
                    cappottiReceived = lu?.cappottiReceived ?: 0,
                    role = player.role
                )
            }

            // Update State all at once
            _state.value = _state.value.copy(
                playerRankings = playersRank,
                teamRankings = teamsRank,
                seasonMatches = matches,
                seasonTeams = teams,
                competitionPlayers = compPlayers,
                seasonUsers = enrichedUsers,
                loading = false
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Errore caricamento dati: ${e.getErrorMessage()}")
        }
    }

    fun loadCompetitionPlayers(competitionId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { ApiClientBase.competitions.getCompetitionPlayers(competitionId) }
            .onSuccess { players ->
                _state.value = _state.value.copy(
                    competitionPlayers = players,
                    loading = false
                )
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore partecipanti: ${e.getErrorMessage()}")
            }
    }

    fun loadLeagueMembersForParticipantPicker(leagueId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { ApiClientBase.leagues.getLeagueMembersForCompetitionPicker(leagueId) }
            .onSuccess { members ->
                _state.value = _state.value.copy(leagueMembers = members, loading = false)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore membri lega: ${e.getErrorMessage()}")
            }
    }

    fun addCompetitionPlayer(competitionId: Long, userId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.addCompetitionPlayer(competitionId, userId) }
            .onSuccess {
                _state.value = _state.value.copy(successMessage = "Partecipante aggiunto", loading = false)
                loadCompetitionPlayers(competitionId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore aggiunta partecipante: ${e.getErrorMessage()}")
            }
    }

    fun removeCompetitionPlayer(competitionId: Long, userId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.removeCompetitionPlayer(competitionId, userId) }
            .onSuccess {
                _state.value = _state.value.copy(successMessage = "Partecipante rimosso", loading = false)
                loadCompetitionPlayers(competitionId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore rimozione partecipante: ${e.getErrorMessage()}")
            }
    }

    fun loadRankings(competitionId: Long, rankingMode: String? = null) = viewModelScope.launch {
        val resolvedRankingMode = rankingMode ?: rankingModeForCompetition(competitionId)
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            val players = if (resolvedRankingMode != "TEAM") {
                ApiClientBase.competitions.getPlayerRankings(competitionId)
            } else {
                emptyList()
            }
            val teams = if (resolvedRankingMode != "PLAYER") {
                ApiClientBase.competitions.getTeamRankings(competitionId)
            } else {
                emptyList()
            }
            players to teams
        }.onSuccess { (players, teams) ->
            _state.value = _state.value.copy(
                playerRankings = players,
                teamRankings = teams,
                loading = false
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Errore classifiche: ${e.getErrorMessage()}")
        }
    }

    private fun rankingModeForCompetition(competitionId: Long): String {
        val state = _state.value
        return state.currentCompetition?.takeIf { it.id == competitionId }?.rankingMode
            ?: state.competitions.firstOrNull { it.id == competitionId }?.rankingMode
            ?: "BOTH"
    }

    fun loadCompetitionMatches(competitionId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { ApiClientBase.competitions.getMatches(competitionId) }
            .onSuccess { matches ->
                _state.value = _state.value.copy(
                    seasonMatches = matches,
                    loading = false
                )
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore partite: ${e.getErrorMessage()}")
            }
    }

    fun joinCompetition(league: LeagueResponse, season: SeasonResponse, competitionId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.joinCompetition(competitionId) }
            .onSuccess {
                _state.value = _state.value.copy(
                    successMessage = "Iscrizione completata con successo!",
                    loading = false
                )
                // Refresh competitions list to update joined status
                selectSeason(league, season)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore durante l'iscrizione: ${e.getErrorMessage()}")
            }
    }

    fun loadSeasonStatsData(leagueId: Long, seasonId: Long, competitionId: Long? = null) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching {
            val teams = if (competitionId != null) {
                ApiClientBase.competitions.getCompetitionTeams(competitionId)
            } else {
                emptyList()
            }
            val users = if (competitionId != null) {
                val compPlayers = ApiClientBase.competitions.getCompetitionPlayers(competitionId)
                val leagueUsers = ApiClientBase.leagues.getLeagueUsers(leagueId)
                compPlayers.map { player ->
                    val lu = leagueUsers.find { it.userId == player.userId }
                    LeagueUserResponse(
                        userId = player.userId,
                        username = player.username,
                        email = player.email,
                        rating = lu?.rating ?: 0,
                        goalsFor = lu?.goalsFor ?: 0,
                        goalsAgainst = lu?.goalsAgainst ?: 0,
                        matchesPlayed = lu?.matchesPlayed ?: 0,
                        cappottiGiven = lu?.cappottiGiven ?: 0,
                        cappottiReceived = lu?.cappottiReceived ?: 0,
                        role = player.role
                    )
                }
            } else {
                ApiClientBase.leagues.getLeagueUsers(leagueId)
            }
            teams to users
        }.onSuccess { (teams, users) ->
            val myUserId = _state.value.currentUser?.userId
            val myRoleInMembers = _state.value.leagueMembers.find { it.userId == myUserId }?.role
            
            _state.value = _state.value.copy(
                seasonTeams = teams,
                seasonUsers = users,
                currentUserRoleInLeague = myRoleInMembers ?: _state.value.currentUserRoleInLeague,
                loading = false
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Errore caricamento dati: ${e.getErrorMessage()}")
        }
    }

    fun loadTeamDetailData(competitionId: Long, teamId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching {
            val team = ApiClientBase.competitions.getTeam(competitionId, teamId)
            val stats = ApiClientBase.competitions.getTeamStatistics(competitionId, teamId)
            val history = ApiClientBase.competitions.getTeamRatingHistory(competitionId, teamId)
            val teamMatches = ApiClientBase.competitions.getTeamMatches(competitionId, teamId)
            val members = ApiClientBase.competitions.getTeamMembers(teamId)
            
            _state.value = _state.value.copy(
                currentTeamStats = stats,
                currentTeamRatingHistory = history,
                currentTeamMatches = teamMatches,
                teamMembers = members,
                currentCompetition = _state.value.currentCompetition,
                currentLeague = _state.value.currentLeague,
                currentSeason = _state.value.currentSeason,
                currentScreen = Screen.TeamDetail(
                    _state.value.currentLeague!!,
                    _state.value.currentSeason!!,
                    _state.value.currentCompetition!!,
                    team
                ),
                loading = false
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Errore squadra: ${e.getErrorMessage()}")
        }
    }

    fun loadHeadToHead(competitionId: Long, teamAId: Long, teamBId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.headToHead(competitionId, teamAId, teamBId) }
            .onSuccess { _state.value = _state.value.copy(currentTeamHeadToHead = it, loading = false) }
            .onFailure { e -> _state.value = _state.value.copy(loading = false, error = "Errore confronto: ${e.getErrorMessage()}") }
    }

    fun loadPlayerDetailData(competitionId: Long, userId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching {
            val stats = ApiClientBase.competitions.getPlayerStats(competitionId, userId)
            val history = ApiClientBase.competitions.getPlayerRatingHistory(competitionId, userId)
            val partners = ApiClientBase.competitions.getPlayerPartners(competitionId, userId)
            // Note: If there's a getPlayerMatches(competitionId, userId) in the future, it should be called here.
            Triple(stats, history, partners)
        }.onSuccess { (stats, history, partners) ->
            _state.value = _state.value.copy(
                currentPlayerStats = stats,
                currentPlayerRatingHistory = history,
                currentPlayerPartners = partners,
                loading = false
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Errore giocatore: ${e.getErrorMessage()}")
        }
    }

    fun createSeason(leagueId: Long, request: CreateSeasonRequest) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.createSeason(leagueId, request) }
            .onSuccess { 
                _state.value = _state.value.copy(successMessage = "Stagione creata!")
                // Refresh seasons list
                selectLeague(_state.value.currentLeague!!)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore stagione: ${e.getErrorMessage()}")
            }
    }

    fun createCompetition(league: LeagueResponse, season: SeasonResponse, request: CreateCompetitionRequest) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.createCompetition(season.id, request) }
            .onSuccess {
                _state.value = _state.value.copy(successMessage = "Competizione creata!")
                selectSeason(league, season)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore competizione: ${e.getErrorMessage()}")
            }
    }

    fun closeSeason(league: LeagueResponse, season: SeasonResponse) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.closeSeason(season.id) }
            .onSuccess { updatedSeason ->
                _state.value = _state.value.copy(
                    successMessage = "Stagione chiusa con successo!",
                    loading = false,
                    currentSeason = updatedSeason
                )
                // Refresh list
                selectLeague(league)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore chiusura: ${e.getErrorMessage()}")
            }
    }

    fun closeCompetition(league: LeagueResponse, season: SeasonResponse, competition: CompetitionResponse) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.closeCompetition(competition.id) }
            .onSuccess { updatedCompetition ->
                _state.value = _state.value.copy(
                    successMessage = "Competizione chiusa con successo!",
                    loading = false,
                    currentCompetition = updatedCompetition
                )
                // Refresh list to show updated status
                selectSeason(league, season)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore chiusura competizione: ${e.getErrorMessage()}")
            }
    }

    fun recalculateCompetition(competitionId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.recalculateCompetition(competitionId) }
            .onSuccess {
                _state.value = _state.value.copy(
                    successMessage = "Ricalcolo punti completato!",
                    loading = false
                )
                // Refresh rankings
                loadRankings(competitionId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore durante il ricalcolo: ${e.getErrorMessage()}")
            }
    }

    fun generateCalendar(competitionId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.generateCalendar(competitionId) }
            .onSuccess {
                _state.value = _state.value.copy(
                    successMessage = "Calendario generato con successo!",
                    loading = false
                )
                // Refresh data
                loadCompetitionMatches(competitionId)
                loadRankings(competitionId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore generazione calendario: ${e.getErrorMessage()}")
            }
    }

    fun generateBracket(competitionId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.generateBracket(competitionId) }
            .onSuccess {
                _state.value = _state.value.copy(
                    successMessage = "Tabellone generato con successo!",
                    loading = false
                )
                // Refresh data
                loadCompetitionMatches(competitionId)
                loadRankings(competitionId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore generazione tabellone: ${e.getErrorMessage()}")
            }
    }

    fun closeLeague(leagueId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.closeLeague(leagueId) }
            .onSuccess {
                _state.value = _state.value.copy(
                    successMessage = "Lega chiusa con successo!",
                    loading = false,
                    currentScreen = Screen.MyLeagues
                )
                loadMyLeagues()
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore chiusura lega: ${e.getErrorMessage()}")
            }
    }

    fun updateMemberRole(leagueId: Long, userId: Long, role: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.updateMemberRole(leagueId, userId, UpdateLeagueMemberRoleRequest(role)) }
            .onSuccess {
                _state.value = _state.value.copy(
                    successMessage = "Ruolo aggiornato con successo!",
                    loading = false
                )
                _state.value.currentLeague?.let { selectLeague(it) }
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore aggiornamento ruolo: ${e.getErrorMessage()}")
            }
    }

    fun createGuestPlayer(seasonId: Long, username: String, onCreated: (LeagueUserResponse) -> Unit) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.createGuestPlayer(seasonId, CreateGuestPlayerRequest(username)) }
            .onSuccess { user ->
                _state.value = _state.value.copy(
                    successMessage = "Giocatore Guest creato!",
                    loading = false
                )
                // Refresh user list for the current UI context (stats or team creation)
                val currentLeague = _state.value.currentLeague
                val currentSeason = _state.value.currentSeason
                val currentCompetition = _state.value.currentCompetition
                if (currentLeague != null && currentSeason != null) {
                    loadSeasonStatsData(currentLeague.id, currentSeason.id, currentCompetition?.id)
                }
                onCreated(user)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore creazione guest: ${e.getErrorMessage()}")
            }
    }

    fun createMatch(competitionId: Long, request: CreateDoubleMatchRequest) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.createMatch(competitionId, request) }
            .onSuccess { 
                _state.value = _state.value.copy(successMessage = "Partita registrata!")
                loadCompetitionMatches(competitionId)
                loadRankings(competitionId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore partita: ${e.getErrorMessage()}")
            }
    }

    fun deleteMatch(competitionId: Long, matchId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.deleteMatch(competitionId, matchId) }
            .onSuccess {
                _state.value = _state.value.copy(successMessage = "Partita eliminata!", loading = false)
                loadCompetitionMatches(competitionId)
                loadRankings(competitionId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore eliminazione partita: ${e.getErrorMessage()}")
            }
    }

    fun updateMatchResult(competitionId: Long, matchId: Long, scoreA: Int, scoreB: Int) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.matches.updateMatchResult(matchId, UpdateMatchResultRequest(scoreA, scoreB)) }
            .onSuccess {
                _state.value = _state.value.copy(successMessage = "Risultato aggiornato!", loading = false)
                loadCompetitionMatches(competitionId)
                loadRankings(competitionId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore aggiornamento risultato: ${e.getErrorMessage()}")
            }
    }

    fun createTeam(competitionId: Long, request: CreateTeamRequest, onSuccess: (() -> Unit)? = null) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.createCompetitionTeam(competitionId, request) }
            .onSuccess { 
                _state.value = _state.value.copy(successMessage = "Squadra creata!")
                val currentLeague = _state.value.currentLeague
                val currentSeason = _state.value.currentSeason
                if (currentLeague != null && currentSeason != null) {
                    loadSeasonStatsData(currentLeague.id, currentSeason.id, competitionId)
                }
                onSuccess?.invoke()
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore squadra: ${e.getErrorMessage()}")
            }
    }

    fun loadTeamMembers(teamId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { ApiClientBase.competitions.getTeamMembers(teamId) }
            .onSuccess { members ->
                _state.value = _state.value.copy(teamMembers = members, loading = false)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore membri squadra: ${e.getErrorMessage()}")
            }
    }

    fun joinTeam(inviteCode: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.competitions.joinTeamWithInviteCode(inviteCode) }
            .onSuccess {
                _state.value = _state.value.copy(successMessage = "Sei entrato nella squadra!", loading = false)
                val comp = _state.value.currentCompetition
                val league = _state.value.currentLeague
                val season = _state.value.currentSeason
                if (comp != null && league != null && season != null) {
                    selectCompetition(league, season, comp)
                }
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore ingresso squadra: ${e.getErrorMessage()}")
            }
    }

    fun createLeague() = viewModelScope.launch {
        val s = _state.value
        if (s.newLeagueName.isBlank()) {
            _state.value = s.copy(error = "Il nome della lega è obbligatorio")
            return@launch
        }
        _state.value = s.copy(loading = true, error = null, successMessage = null)
        runCatching { 
            ApiClientBase.leagues.createLeague(CreateLeagueRequest(s.newLeagueName, s.newLeagueDescription))
        }.onSuccess { league -> 
            _state.value = _state.value.copy(
                loading = false,
                successMessage = "Lega creata con successo!",
                newLeagueName = "",
                newLeagueDescription = ""
            )
            selectLeague(league)
            loadMyLeagues()
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Errore creazione lega: ${e.getErrorMessage()}")
        }
    }

    fun uploadLeagueCover(leagueId: Long, uri: Uri) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching {
            val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: throw Exception("Impossibile leggere il file")
            val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", "cover.jpg", requestFile)
            
            ApiClientBase.leagues.uploadLeagueCover(leagueId, body)
        }.onSuccess { league ->
            _state.value = _state.value.copy(
                loading = false,
                successMessage = "Immagine caricata con successo!",
                currentLeague = league
            )
            // Se necessario ricarichiamo le altre liste
            loadMyLeagues()
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Errore caricamento immagine: ${e.getErrorMessage()}")
        }
    }

    fun deleteLeagueCover(leagueId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.deleteLeagueCover(leagueId) }
            .onSuccess { league ->
                _state.value = _state.value.copy(
                    loading = false,
                    successMessage = "Immagine rimossa!",
                    currentLeague = league
                )
                loadMyLeagues()
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore rimozione immagine: ${e.getErrorMessage()}")
            }
    }

    fun joinLeague(code: String? = null) = viewModelScope.launch {
        val s = _state.value
        val finalCode = code ?: s.inviteCode
        if (finalCode.isBlank()) {
            _state.value = s.copy(error = "Il codice di invito è obbligatorio")
            return@launch
        }
        _state.value = s.copy(loading = true, error = null, successMessage = null)
        runCatching {
            ApiClientBase.leagues.joinLeague(JoinLeagueRequest(finalCode))
        }.onSuccess { league ->
            _state.value = _state.value.copy(
                loading = false,
                successMessage = "Ti sei unito alla lega!",
                inviteCode = ""
            )
            selectLeague(league)
            loadMyLeagues()
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Errore durante l'accesso alla lega: ${e.getErrorMessage()}")
        }
    }

    fun loadCurrentUser() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { ApiClientBase.userSettings.getMe() }
            .onSuccess { me ->
                val currentAuth = _state.value.currentUser
                _state.value = _state.value.copy(
                    currentUser = AuthResponse(
                        jwt = currentAuth?.jwt,
                        refreshToken = currentAuth?.refreshToken,
                        userId = me.id,
                        name = me.username ?: "Utente",
                        email = me.email
                    ),
                    loading = false
                )
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Impossibile caricare il profilo")
                Log.e("AppViewModel", "Failed to load current user", e)
            }
    }

    fun updateProfile(name: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.userSettings.updateProfile(UpdateProfileRequest(username = name)) }
            .onSuccess { me ->
                val currentAuth = _state.value.currentUser
                _state.value = _state.value.copy(
                    currentUser = AuthResponse(
                        jwt = currentAuth?.jwt,
                        refreshToken = currentAuth?.refreshToken,
                        userId = me.id,
                        name = me.username ?: "Utente",
                        email = me.email
                    ),
                    loading = false,
                    successMessage = "Profilo aggiornato con successo!"
                )
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore aggiornamento profilo: ${e.getErrorMessage()}")
            }
    }

    fun changePassword(old: String, new: String) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.userSettings.changePassword(ChangePasswordRequest(old, new)) }
            .onSuccess {
                _state.value = _state.value.copy(
                    loading = false,
                    successMessage = "Password cambiata con successo!"
                )
                // Se la password è cambiata, aggiorniamo le credenziali per il biometrico
                val currentEmail = _state.value.email
                if (currentEmail.isNotBlank()) {
                    sessionManager.saveCredentials(currentEmail, new)
                }
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore cambio password: ${e.getErrorMessage()}")
            }
    }

    fun logout() {
        val canBio = _state.value.canUseBiometrics
        val bioEnabled = _state.value.isBiometricEnabled
        viewModelScope.launch { sessionManager.clearSession() }
        ApiClientBase.authToken = null
        _state.value = UiState(currentScreen = Screen.PublicLeagues).copy(
            canUseBiometrics = canBio,
            isBiometricEnabled = bioEnabled
        )
        loadPublicLeagues()
    }

    fun showBiometricPrompt(activity: FragmentActivity) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val (email, password) = sessionManager.getCredentials()
                if (email != null && password != null) {
                    _state.value = _state.value.copy(email = email, password = password)
                    login()
                } else {
                    _state.value = _state.value.copy(error = "Nessuna credenziale salvata. Effettua il primo login manualmente.")
                }
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                _state.value = _state.value.copy(error = errString.toString())
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login Biometrico")
            .setSubtitle("Usa l'impronta digitale o il volto per accedere")
            .setNegativeButtonText("Annulla")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun Throwable.getErrorMessage(): String {
        val errorMsg = if (this is HttpException) {
            val code = code()
            Log.w("AppViewModel", "HTTP Error $code: ${message()}")
            try {
                val errorBody = response()?.errorBody()?.string()
                if (!errorBody.isNullOrBlank()) {
                    val trimmedBody = errorBody.trim()
                    if (trimmedBody.startsWith("{")) {
                        try {
                            val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(trimmedBody).jsonObject
                            json["message"]?.jsonPrimitive?.content
                                ?: json["error"]?.jsonPrimitive?.content
                                ?: "Errore $code: $trimmedBody"
                        } catch (e: Exception) {
                            "Errore $code: $trimmedBody"
                        }
                    } else {
                        // Se non è un JSON, mostra il testo puro (spesso il backend manda stringhe semplici per gli errori)
                        "Errore: $trimmedBody"
                    }
                } else {
                    "Errore del server ($code)"
                }
            } catch (_: Exception) {
                "Errore di rete ($code)"
            }
        } else {
            Log.e("AppViewModel", "getErrorMessage triggered for: ${this.javaClass.simpleName}", this)
            message ?: "Errore sconosciuto"
        }

        Log.w("AppViewModel", "Final error message to display: $errorMsg")
        return errorMsg
    }
}
