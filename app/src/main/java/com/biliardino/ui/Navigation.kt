package com.biliardino.ui

import com.biliardino.model.LeagueResponse
import com.biliardino.model.LeagueUserResponse
import com.biliardino.model.SeasonResponse
import com.biliardino.model.TeamResponse

sealed class Screen {
    object Splash : Screen()
    object PublicLeagues : Screen()
    object AuthMenu : Screen()
    object MyLeagues : Screen()
    data class LeagueSeasons(val league: LeagueResponse) : Screen()
    data class SeasonDetail(val league: LeagueResponse, val season: SeasonResponse) : Screen()
    data class SeasonStatistics(val league: LeagueResponse, val season: SeasonResponse) : Screen()
    data class SeasonMatches(val league: LeagueResponse, val season: SeasonResponse) : Screen()
    data class SeasonTeams(val league: LeagueResponse, val season: SeasonResponse) : Screen()
    data class TeamDetail(val league: LeagueResponse, val season: SeasonResponse, val team: TeamResponse) : Screen()
    data class PlayerDetail(val league: LeagueResponse, val season: SeasonResponse, val user: LeagueUserResponse) : Screen()
}
