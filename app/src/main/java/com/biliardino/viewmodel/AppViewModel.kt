package com.biliardino.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biliardino.model.*
import com.biliardino.network.ApiClientBase
import com.biliardino.network.SessionManager
import com.biliardino.ui.Screen
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

data class UiState(
    val currentUser: AuthResponse? = null,
    val currentScreen: Screen = Screen.Splash,
    val publicLeagues: List<LeagueResponse> = emptyList(),
    val myLeagues: List<LeagueResponse> = emptyList(),
    val currentLeague: LeagueResponse? = null,
    val seasons: List<SeasonResponse> = emptyList(),
    val currentSeason: SeasonResponse? = null,
    val playerRankings: List<PlayerRankingResponse> = emptyList(),
    val teamRankings: List<TeamRankingResponse> = emptyList(),
    val seasonMatches: List<MatchResponse> = emptyList(),
    val seasonTeams: List<TeamResponse> = emptyList(),
    val seasonUsers: List<LeagueUserResponse> = emptyList(),
    val currentTeamStats: TeamStatsResponse? = null,
    val currentTeamRatingHistory: List<RatingHistoryResponse> = emptyList(),
    val currentTeamHeadToHead: HeadToHeadResponse? = null,
    val currentPlayerStats: PlayerStatsResponse? = null,
    val currentPlayerRatingHistory: List<RatingHistoryResponse> = emptyList(),
    val currentPlayerPartners: List<PlayerPartnerStatsResponse> = emptyList(),
    
    // Auth fields
    val email: String = "",
    val password: String = "",
    val username: String = "",
    
    val loading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private val sessionManager = SessionManager(application)

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            checkSession()
            loadPublicLeagues()
        }
    }

    private fun checkSession() = viewModelScope.launch {
        val token = sessionManager.authToken.first()
        if (token != null) {
            ApiClientBase.authToken = token
            // Optionally fetch user info here if needed
            _state.value = _state.value.copy(currentScreen = Screen.MyLeagues)
            loadMyLeagues()
        } else {
            _state.value = _state.value.copy(currentScreen = Screen.PublicLeagues)
        }
    }

    fun onEmailChange(v: String) { _state.value = _state.value.copy(email = v) }
    fun onPasswordChange(v: String) { _state.value = _state.value.copy(password = v) }
    fun onUsernameChange(v: String) { _state.value = _state.value.copy(username = v) }

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
            ApiClientBase.authToken = auth.token
            auth.token?.let { sessionManager.saveToken(it) }
            _state.value = _state.value.copy(
                currentUser = auth,
                loading = false,
                currentScreen = Screen.MyLeagues,
                successMessage = "Login effettuato con successo!"
            )
            loadMyLeagues()
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
            ApiClientBase.authToken = auth.token
            auth.token?.let { sessionManager.saveToken(it) }
            _state.value = _state.value.copy(
                currentUser = auth,
                loading = false,
                currentScreen = Screen.MyLeagues,
                successMessage = "Registrazione completata!"
            )
            loadMyLeagues()
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Registrazione fallita: ${e.getErrorMessage()}")
        }
    }

    fun loadPublicLeagues() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.getPublicLeagues() }
            .onSuccess { 
                _state.value = _state.value.copy(
                    publicLeagues = it, 
                    loading = false,
                    successMessage = "Leghe pubbliche caricate"
                ) 
            }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.getErrorMessage()) }
    }

    fun loadMyLeagues() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.getMyLeagues() }
            .onSuccess { 
                _state.value = _state.value.copy(
                    myLeagues = it, 
                    loading = false,
                    successMessage = "Le tue leghe caricate"
                ) 
            }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.getErrorMessage()) }
    }

    fun selectLeague(league: LeagueResponse) = viewModelScope.launch {
        _state.value = _state.value.copy(currentLeague = league, loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.getSeasons(league.id) }
            .onSuccess { 
                _state.value = _state.value.copy(
                    seasons = it, 
                    loading = false,
                    currentScreen = Screen.LeagueSeasons(league),
                    successMessage = "Stagioni caricate"
                ) 
            }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.getErrorMessage()) }
    }

    fun selectSeason(league: LeagueResponse, season: SeasonResponse) {
        _state.value = _state.value.copy(
            currentSeason = season,
            currentScreen = Screen.SeasonStatistics(league, season)
        )
        loadRankings(season.id)
        loadSeasonStatsData(league.id, season.id)
    }

    fun loadRankings(seasonId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching {
            val players = ApiClientBase.matches.getPlayerRankings(seasonId)
            val teams = ApiClientBase.matches.getTeamRankings(seasonId)
            players to teams
        }.onSuccess { (players, teams) ->
            _state.value = _state.value.copy(
                playerRankings = players,
                teamRankings = teams,
                loading = false,
                successMessage = "Classifiche aggiornate"
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Errore classifiche: ${e.getErrorMessage()}")
        }
    }

    fun loadSeasonStatsData(leagueId: Long, seasonId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching {
            val matches = ApiClientBase.matches.getMatches(seasonId)
            val teams = ApiClientBase.matches.getTeams(seasonId)
            val users = ApiClientBase.leagues.getLeagueUsers(leagueId)
            Triple(matches, teams, users)
        }.onSuccess { (matches, teams, users) ->
            _state.value = _state.value.copy(
                seasonMatches = matches,
                seasonTeams = teams,
                seasonUsers = users,
                loading = false,
                successMessage = "Dati stagione aggiornati"
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Errore dati: ${e.getErrorMessage()}")
        }
    }

    fun loadTeamDetailData(seasonId: Long, teamId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching {
            val stats = ApiClientBase.matches.getTeamStats(seasonId, teamId)
            val history = ApiClientBase.matches.getTeamRatingHistory(seasonId, teamId)
            stats to history
        }.onSuccess { (stats, history) ->
            _state.value = _state.value.copy(
                currentTeamStats = stats,
                currentTeamRatingHistory = history,
                loading = false,
                successMessage = "Dettagli squadra caricati"
            )
        }.onFailure { e ->
            _state.value = _state.value.copy(loading = false, error = "Errore squadra: ${e.getErrorMessage()}")
        }
    }

    fun loadHeadToHead(seasonId: Long, teamAId: Long, teamBId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.matches.headToHead(seasonId, teamAId, teamBId) }
            .onSuccess { _state.value = _state.value.copy(currentTeamHeadToHead = it, loading = false, successMessage = "Confronto caricato") }
            .onFailure { e -> _state.value = _state.value.copy(loading = false, error = "Errore confronto: ${e.getErrorMessage()}") }
    }

    fun loadPlayerDetailData(seasonId: Long, userId: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching {
            val stats = ApiClientBase.matches.getPlayerStats(seasonId, userId)
            val history = ApiClientBase.matches.getPlayerRatingHistory(seasonId, userId)
            val partners = ApiClientBase.matches.getPlayerPartners(seasonId, userId)
            Triple(stats, history, partners)
        }.onSuccess { (stats, history, partners) ->
            _state.value = _state.value.copy(
                currentPlayerStats = stats,
                currentPlayerRatingHistory = history,
                currentPlayerPartners = partners,
                loading = false,
                successMessage = "Dettagli giocatore caricati"
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

    fun createGuestPlayer(leagueId: Long, username: String, onCreated: (LeagueUserResponse) -> Unit) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.leagues.createGuestPlayer(leagueId, CreateGuestPlayerRequest(username)) }
            .onSuccess { user ->
                _state.value = _state.value.copy(
                    successMessage = "Giocatore Guest creato!",
                    loading = false
                )
                // Refresh user list for the current UI context (stats or team creation)
                val currentLeague = _state.value.currentLeague
                val currentSeason = _state.value.currentSeason
                if (currentLeague != null && currentSeason != null) {
                    loadSeasonStatsData(currentLeague.id, currentSeason.id)
                }
                onCreated(user)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore creazione guest: ${e.getErrorMessage()}")
            }
    }

    fun createMatch(seasonId: Long, request: CreateDoubleMatchRequest) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.matches.createMatch(seasonId, request) }
            .onSuccess { 
                _state.value = _state.value.copy(successMessage = "Partita registrata!")
                loadSeasonStatsData(_state.value.currentLeague!!.id, seasonId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore partita: ${e.getErrorMessage()}")
            }
    }

    fun createTeam(seasonId: Long, request: CreateTeamRequest) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
        runCatching { ApiClientBase.matches.createTeam(seasonId, request) }
            .onSuccess { 
                _state.value = _state.value.copy(successMessage = "Squadra creata!")
                loadSeasonStatsData(_state.value.currentLeague!!.id, seasonId)
            }
            .onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = "Errore squadra: ${e.getErrorMessage()}")
            }
    }

    fun logout() {
        viewModelScope.launch { sessionManager.clearSession() }
        ApiClientBase.authToken = null
        _state.value = UiState()
        loadPublicLeagues()
    }

    private fun Throwable.getErrorMessage(): String {
        val errorMsg = if (this is HttpException) {
            try {
                val errorBody = response()?.errorBody()?.string()
                if (!errorBody.isNullOrBlank()) {
                    val trimmedBody = errorBody.trim()
                    if (trimmedBody.startsWith("{")) {
                        try {
                            val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(trimmedBody).jsonObject
                            json["message"]?.jsonPrimitive?.content
                                ?: json["error"]?.jsonPrimitive?.content
                                ?: errorBody
                        } catch (e: Exception) {
                            errorBody
                        }
                    } else {
                        errorBody
                    }
                } else {
                    "Errore del server (${code()})"
                }
            } catch (_: Exception) {
                "Errore di rete (${code()})"
            }
        } else {
            message ?: "Errore sconosciuto"
        }

        if (this is HttpException && code() >= 500) {
            Log.e("AppViewModel", "Errore Server (${code()}): $errorMsg", this)
        } else {
            Log.w("AppViewModel", "Errore intercettato: $errorMsg")
        }
        
        return errorMsg
    }
}
