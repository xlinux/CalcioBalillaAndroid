package com.biliardino.ui

import com.biliardino.model.CompetitionResponse
import com.biliardino.model.LeagueResponse
import com.biliardino.model.LeagueUserResponse
import com.biliardino.model.SeasonResponse
import com.biliardino.model.TeamResponse

sealed class Screen {
    object Splash : Screen()
    object PublicLeagues : Screen()
    object AuthMenu : Screen()
    object MyLeagues : Screen()
    object Profile : Screen()
    data class LeagueSeasons(val league: LeagueResponse) : Screen()
    data class SeasonCompetitions(val league: LeagueResponse, val season: SeasonResponse) : Screen()
    data class CompetitionStatistics(val league: LeagueResponse, val season: SeasonResponse, val competition: CompetitionResponse) : Screen()
    data class CompetitionMatches(val league: LeagueResponse, val season: SeasonResponse, val competition: CompetitionResponse) : Screen()
    data class SeasonTeams(val league: LeagueResponse, val season: SeasonResponse, val competition: CompetitionResponse? = null) : Screen()
    data class SeasonSettings(val league: LeagueResponse, val season: SeasonResponse, val competition: CompetitionResponse? = null) : Screen()
    data class TeamDetail(val league: LeagueResponse, val season: SeasonResponse, val competition: CompetitionResponse, val team: TeamResponse) : Screen()
    data class PlayerDetail(val league: LeagueResponse, val season: SeasonResponse, val competition: CompetitionResponse, val user: LeagueUserResponse) : Screen()
}
