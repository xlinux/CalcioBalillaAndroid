package com.biliardino.network

import com.biliardino.model.*
import okhttp3.MultipartBody
import retrofit2.http.*

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): AuthResponse
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

    @GET("leagues/{leagueId}/members")
    suspend fun getLeagueMembersForCompetitionPicker(@Path("leagueId") leagueId: Long): List<LeagueMemberResponse>

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

interface SportApi {
    @GET("sports")
    suspend fun getSports(): List<SportResponse>

    @GET("competition-templates")
    suspend fun getCompetitionTemplates(): List<CompetitionTemplateResponse>
}

interface CompetitionApi {
    @GET("competitions/{competitionId}/players")
    suspend fun getCompetitionPlayers(@Path("competitionId") competitionId: Long): List<LeagueMemberResponse>

    @POST("competitions/{competitionId}/players/{userId}")
    suspend fun addCompetitionPlayer(@Path("competitionId") competitionId: Long, @Path("userId") userId: Long)

    @DELETE("competitions/{competitionId}/players/{userId}")
    suspend fun removeCompetitionPlayer(@Path("competitionId") competitionId: Long, @Path("userId") userId: Long)

    @GET("competitions/{competitionId}/teams")
    suspend fun getCompetitionTeams(@Path("competitionId") competitionId: Long): List<TeamResponse>

    @POST("competitions/{competitionId}/teams")
    suspend fun createCompetitionTeam(@Path("competitionId") competitionId: Long, @Body request: CreateTeamRequest): TeamResponse

    @GET("competitions/{competitionId}/teams/{teamId}")
    suspend fun getTeam(@Path("competitionId") competitionId: Long, @Path("teamId") teamId: Long): TeamResponse

    @GET("teams/{teamId}/members")
    suspend fun getTeamMembers(@Path("teamId") teamId: Long): List<TeamMemberResponse>

    @POST("teams/join/{inviteCode}")
    suspend fun joinTeamWithInviteCode(@Path("inviteCode") inviteCode: String): JoinTeamResponse

    @GET("competitions/{competitionId}/partite")
    suspend fun getMatches(@Path("competitionId") competitionId: Long): List<MatchResponse>

    @POST("competitions/{competitionId}/partite")
    suspend fun createMatch(@Path("competitionId") competitionId: Long, @Body request: CreateDoubleMatchRequest): MatchResponse

    @DELETE("competitions/{competitionId}/partite/{matchId}")
    suspend fun deleteMatch(@Path("competitionId") competitionId: Long, @Path("matchId") matchId: Long)

    @GET("competitions/{competitionId}/classifica-squadre")
    suspend fun getTeamRankings(@Path("competitionId") competitionId: Long): List<TeamRankingResponse>

    @GET("competitions/{competitionId}/classifica-giocatori")
    suspend fun getPlayerRankings(@Path("competitionId") competitionId: Long): List<PlayerRankingResponse>

    @GET("competitions/{competitionId}/partite/squadre/{teamId}")
    suspend fun getTeamMatches(@Path("competitionId") competitionId: Long, @Path("teamId") teamId: Long): List<MatchResponse>

    @POST("competitions/{competitionId}/join")
    suspend fun joinCompetition(@Path("competitionId") competitionId: Long): CompetitionResponse

    @PUT("competitions/{competitionId}/close")
    suspend fun closeCompetition(@Path("competitionId") competitionId: Long): CompetitionResponse

    @POST("competitions/{competitionId}/recalculate")
    suspend fun recalculateCompetition(@Path("competitionId") competitionId: Long)

    @POST("competitions/{competitionId}/calendar/generate")
    suspend fun generateCalendar(@Path("competitionId") competitionId: Long)

    @POST("competitions/{competitionId}/bracket/generate")
    suspend fun generateBracket(@Path("competitionId") competitionId: Long)

    @GET("competitions/{competitionId}/teams/{teamId}/statistics")
    suspend fun getTeamStatistics(
        @Path("competitionId") competitionId: Long,
        @Path("teamId") teamId: Long
    ): TeamStatsResponse

    @GET("competitions/{competitionId}/teams/{teamId}/rating-history")
    suspend fun getTeamRatingHistory(
        @Path("competitionId") competitionId: Long,
        @Path("teamId") teamId: Long
    ): List<RatingHistoryResponse>

    @GET("competitions/{competitionId}/teams/{teamAId}/head-to-head/{teamBId}")
    suspend fun headToHead(
        @Path("competitionId") competitionId: Long,
        @Path("teamAId") teamAId: Long,
        @Path("teamBId") teamBId: Long
    ): HeadToHeadResponse

    @GET("competitions/{competitionId}/players/{userId}/statistics")
    suspend fun getPlayerStats(@Path("competitionId") competitionId: Long, @Path("userId") userId: Long): PlayerStatsResponse

    @GET("competitions/{competitionId}/players/{userId}/partners")
    suspend fun getPlayerPartners(@Path("competitionId") competitionId: Long, @Path("userId") userId: Long): List<PlayerPartnerStatsResponse>

    @GET("competitions/{competitionId}/players/{userId}/rating-history")
    suspend fun getPlayerRatingHistory(@Path("competitionId") competitionId: Long, @Path("userId") userId: Long): List<RatingHistoryResponse>

    @GET("competitions/{competitionId}/players/rating-history")
    suspend fun getAllPlayersRatingHistory(@Path("competitionId") competitionId: Long): List<RatingHistoryResponse>
}

interface MatchApi {
    @POST("stagioni/{seasonId}/partite/random")
    suspend fun generateRandomMatches(@Path("seasonId") seasonId: Long, @Body request: GenerateRandomMatchesRequest): List<MatchResponse>

    @PUT("competitions/matches/{matchId}/result")
    suspend fun updateMatchResult(@Path("matchId") matchId: Long, @Body request: UpdateMatchResultRequest): MatchResponse
}
