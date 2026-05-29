package com.biliardino.network

import com.biliardino.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
}

interface UserSettingsApi {
    @GET("me")
    suspend fun getMe(): MeResponse

    @PUT("me/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): MeResponse

    @PUT("me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest)
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

    @PUT("stagioni/{seasonId}/close")
    suspend fun closeSeason(@Path("seasonId") seasonId: Long): SeasonResponse

    @PUT("leghe/{leagueId}/close")
    suspend fun closeLeague(@Path("leagueId") leagueId: Long): LeagueResponse

    @GET("leghe/{leagueId}/membri")
    suspend fun getLeagueMembers(@Path("leagueId") leagueId: Long): List<LeagueMemberResponse>

    @PUT("leghe/{leagueId}/membri/{userId}/role")
    suspend fun updateMemberRole(
        @Path("leagueId") leagueId: Long,
        @Path("userId") userId: Long,
        @Body request: UpdateLeagueMemberRoleRequest
    ): LeagueMemberResponse

    @Multipart
    @POST("leghe/{leagueId}/cover")
    suspend fun uploadLeagueCover(
        @Path("leagueId") leagueId: Long,
        @Part file: MultipartBody.Part
    ): LeagueResponse

    @DELETE("leghe/{leagueId}/cover")
    suspend fun deleteLeagueCover(@Path("leagueId") leagueId: Long): LeagueResponse

    @POST("stagioni/{seasonId}/guest-player")
    suspend fun createGuestPlayer(@Path("seasonId") seasonId: Long, @Body request: CreateGuestPlayerRequest): LeagueUserResponse

    @GET("stagioni/{seasonId}/competizioni")
    suspend fun getCompetitions(@Path("seasonId") seasonId: Long): List<CompetitionResponse>

    @POST("stagioni/{seasonId}/competizioni")
    suspend fun createCompetition(@Path("seasonId") seasonId: Long, @Body request: CreateCompetitionRequest): CompetitionResponse
}

interface CompetitionApi {
    @GET("competizioni/{competitionId}/partite")
    suspend fun getMatches(@Path("competitionId") competitionId: Long): List<MatchResponse>

    @POST("competizioni/{competitionId}/partite")
    suspend fun createMatch(@Path("competitionId") competitionId: Long, @Body request: CreateDoubleMatchRequest): MatchResponse

    @DELETE("competizioni/{competitionId}/partite/{matchId}")
    suspend fun deleteMatch(@Path("competitionId") competitionId: Long, @Path("matchId") matchId: Long)

    @GET("competizioni/{competitionId}/classifica-squadre")
    suspend fun getTeamRankings(@Path("competitionId") competitionId: Long): List<TeamRankingResponse>

    @GET("competizioni/{competitionId}/classifica-giocatori")
    suspend fun getPlayerRankings(@Path("competitionId") competitionId: Long): List<PlayerRankingResponse>

    @GET("competizioni/{competitionId}/partite/squadre/{teamId}")
    suspend fun getTeamMatches(@Path("competitionId") competitionId: Long, @Path("teamId") teamId: Long): List<MatchResponse>

    @POST("competizioni/{competitionId}/join")
    suspend fun joinCompetition(@Path("competitionId") competitionId: Long): CompetitionResponse

    @PUT("competizioni/{competitionId}/close")
    suspend fun closeCompetition(@Path("competitionId") competitionId: Long): CompetitionResponse

    @POST("competizioni/{competitionId}/recalculate")
    suspend fun recalculateCompetition(@Path("competitionId") competitionId: Long)
}

interface MatchApi {
    @POST("stagioni/{seasonId}/squadre")
    suspend fun createTeam(@Path("seasonId") seasonId: Long, @Body request: CreateTeamRequest): TeamResponse

    @GET("stagioni/{seasonId}/squadre")
    suspend fun getTeams(@Path("seasonId") seasonId: Long): List<TeamResponse>

    @POST("stagioni/{seasonId}/partite/random")
    suspend fun generateRandomMatches(@Path("seasonId") seasonId: Long, @Body request: GenerateRandomMatchesRequest): List<MatchResponse>

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
