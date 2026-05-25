package com.biliardino.network

import com.biliardino.model.*
import retrofit2.http.*

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
}

interface LeagueApi {
    @GET("public/leghe")
    suspend fun getPublicLeagues(): List<LeagueResponse>

    @GET("leghe/mie")
    suspend fun getMyLeagues(): List<LeagueResponse>

    @POST("leghe")
    suspend fun createLeague(@Body request: CreateLeagueRequest): LeagueResponse

    @POST("leghe/join")
    suspend fun joinLeague(@Body request: JoinLeagueRequest): LeagueResponse

    @GET("leghe/{leagueId}/utenti")
    suspend fun getLeagueUsers(@Path("leagueId") leagueId: Long): List<LeagueUserResponse>

    @GET("leghe/{leagueId}/stagioni")
    suspend fun getSeasons(@Path("leagueId") leagueId: Long): List<SeasonResponse>

    @POST("leghe/{leagueId}/stagioni")
    suspend fun createSeason(@Path("leagueId") leagueId: Long, @Body request: CreateSeasonRequest): SeasonResponse

    @POST("leghe/{leagueId}/guest-player")
    suspend fun createGuestPlayer(@Path("leagueId") leagueId: Long, @Body request: CreateGuestPlayerRequest): LeagueUserResponse
}

interface MatchApi {
    @POST("stagioni/{seasonId}/squadre")
    suspend fun createTeam(@Path("seasonId") seasonId: Long, @Body request: CreateTeamRequest): TeamResponse

    @GET("stagioni/{seasonId}/squadre")
    suspend fun getTeams(@Path("seasonId") seasonId: Long): List<TeamResponse>

    @POST("stagioni/{seasonId}/partite")
    suspend fun createMatch(@Path("seasonId") seasonId: Long, @Body request: CreateDoubleMatchRequest): MatchResponse

    @GET("stagioni/{seasonId}/partite")
    suspend fun getMatches(@Path("seasonId") seasonId: Long): List<MatchResponse>

    @GET("stagioni/{seasonId}/partite/squadre/{teamId}")
    suspend fun getTeamMatches(@Path("seasonId") seasonId: Long, @Path("teamId") teamId: Long): List<MatchResponse>

    @POST("stagioni/{seasonId}/partite/random")
    suspend fun generateRandomMatches(@Path("seasonId") seasonId: Long, @Body request: GenerateRandomMatchesRequest): List<MatchResponse>

    @GET("stagioni/{seasonId}/classifica-giocatori")
    suspend fun getPlayerRankings(@Path("seasonId") seasonId: Long): List<PlayerRankingResponse>

    @GET("stagioni/{seasonId}/classifica-squadre")
    suspend fun getTeamRankings(@Path("seasonId") seasonId: Long): List<TeamRankingResponse>

    @GET("stagioni/{seasonId}/squadre/{teamId}/rating-history")
    suspend fun getTeamRatingHistory(@Path("seasonId") seasonId: Long, @Path("teamId") teamId: Long): List<RatingHistoryResponse>

    @GET("stagioni/{seasonId}/giocatori/{userId}/rating-history")
    suspend fun getPlayerRatingHistory(@Path("seasonId") seasonId: Long, @Path("userId") userId: Long): List<RatingHistoryResponse>

    @GET("stagioni/{seasonId}/giocatori/{userId}/statistiche")
    suspend fun getPlayerStats(@Path("seasonId") seasonId: Long, @Path("userId") userId: Long): PlayerStatsResponse

    @GET("stagioni/{seasonId}/giocatori/{userId}/compagni")
    suspend fun getPlayerPartners(@Path("seasonId") seasonId: Long, @Path("userId") userId: Long): List<PlayerPartnerStatsResponse>

    @GET("stagioni/{seasonId}/squadre/{teamId}/statistiche")
    suspend fun getTeamStats(@Path("seasonId") seasonId: Long, @Path("teamId") teamId: Long): TeamStatsResponse

    @GET("stagioni/{seasonId}/squadre/{teamAId}/head-to-head/{teamBId}")
    suspend fun headToHead(
        @Path("seasonId") seasonId: Long,
        @Path("teamAId") teamAId: Long,
        @Path("teamBId") teamBId: Long
    ): HeadToHeadResponse
}
