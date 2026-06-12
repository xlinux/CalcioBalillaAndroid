package com.biliardino.model

import kotlinx.serialization.Serializable

// --- Auth ---

@Serializable
data class GoogleLoginRequest(
    val idToken: String
)

@Serializable
data class AuthResponse(
    val token: String? = null,
    val jwt: String? = null, // Keep for backward compatibility if needed temporarily
    val refreshToken: String? = null,
    val userId: Long,
    val name: String? = null,
    val email: String? = null,
    val authProvider: String? = null
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class MeResponse(
    val id: Long,
    val username: String? = null,
    val email: String,
    val authProvider: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val username: String,
    val avatar: String? = null
)

// --- Leghe ---

@Serializable
data class LeagueResponse( // Renamed from League to match Swagger
    val id: Long,
    val name: String,
    val description: String,
    val leagueType: String? = "PRIVATE_LEAGUE",
    val officialClub: Boolean = false,
    val inviteCode: String? = null,
    val coverImageUrl: String? = null,
    val status: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class CreateLeagueRequest(
    val name: String,
    val description: String,
    val leagueType: String = "PRIVATE_LEAGUE",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class UpdateLeagueRequest(
    val name: String,
    val description: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class JoinLeagueRequest(
    val inviteCode: String
)

@Serializable
data class LeagueMemberResponse(
    val userId: Long,
    val username: String,
    val email: String? = null,
    val avatar: String? = null,
    val role: String = "PLAYER"
)

@Serializable
data class UpdateLeagueMemberRoleRequest(
    val role: String
)

@Serializable
data class CreateGuestPlayerRequest(
    val username: String
)

@Serializable
data class LeagueUserResponse( // Renamed from UserResponse
    val userId: Long,
    val username: String,
    val email: String? = null,
    val rating: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val matchesPlayed: Int,
    val cappottiGiven: Int,
    val cappottiReceived: Int,
    val role: String? = null
)

// --- Stagioni ---

@Serializable
data class SeasonResponse( // Renamed from Season
    val id: Long,
    val leagueId: Long,
    val name: String,
    val startDate: String,
    val endDate: String,
    val active: Boolean? = null
)

@Serializable
data class CompetitionResponse(
    val id: Long,
    val seasonId: Long,
    val name: String,
    val type: String? = null,
    val status: String? = null,
    val active: Boolean? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val rankingMode: String = "BOTH",
    val matchType: String = "DOUBLE",
    val competitionRankingType: String = "POINTS",
    val sportId: Long? = null,
    val sportName: String? = null,
    val targetScore: Int? = null,
    val useTargetScore: Boolean = false,
    val allowDraw: Boolean = false,
    val winPoints: Int = 3,
    val drawPoints: Int = 1,
    val lossPoints: Int = 0,
    val cappottoEnabled: Boolean = true,
    val cappottoBonusPoints: Int = 2,
    val matchFormat: String = "POINTS",
    val winByTwo: Boolean = false,
    val homeAndAway: Boolean = false,
    val currentUserJoined: Boolean = false,
    val matchCreationMode: String = "FREE",
    val calendarGenerationMode: String = "ROUNDS",
    val registrationOpen: Boolean = true,
    val tournamentFormat: String? = null, // SINGLE_ELIMINATION, GROUPS_THEN_SINGLE_ELIMINATION
    val winnerTeamId: Long? = null,
    val winnerUserId: Long? = null,
    val closedAt: String? = null,
    val phase: String? = null // SETUP,GROUP_STAGE,     READY_FOR_FINAL_STAGE, ,FINAL_STAGE,  COMPLETED
)

@Serializable
data class MyCompetitionResponse(
    val id: Long,
    val name: String,
    val type: String,
    val phase: String? = null,
    val rankingMode: String,
    val active: Boolean,
    val leagueId: Long,
    val leagueName: String,
    val seasonId: Long,
    val seasonName: String,
    val sportId: Long,
    val sportName: String
)

@Serializable
data class GenerateGroupsRequest(
    val numberOfGroups: Int
)

@Serializable
data class FinalStageStatusResponse(
    val groupsTournament: Boolean,
    val groupsGenerated: Boolean,
    val groupMatchesGenerated: Boolean,
    val groupStageCompleted: Boolean,
    val finalStageGenerated: Boolean,
    val canGenerateFinalStage: Boolean
)

@Serializable
data class GenerateFinalStageRequest(
    val qualifiedPerGroup: Int? = 2
)

@Serializable
data class CompetitionGroupResponse(
    val id: Long,
    val name: String,
    val sortOrder: Int
)

@Serializable
data class GroupRankingResponse(
    val groupId: Long,
    val groupName: String,
    val rankingMode: String,
    val teamRanking: List<TeamRankingResponse>? = null,
    val playerRanking: List<PlayerRankingResponse>? = null
)

@Serializable
data class CreateSeasonRequest(
    val name: String,
    val startDate: String,
    val endDate: String,
    val copyTeamsFromPreviousSeason: Boolean = false
)

@Serializable
data class CreateCompetitionRequest(
    val name: String,
    val type: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val copyFromCompetitionId: Long? = null,
    val copyParticipants: Boolean = false,
    val copyTeams: Boolean = false,
    val rankingMode: String = "BOTH",
    val matchType: String = "DOUBLE",
    val competitionRankingType: String = "POINTS",
    val sportId: Long,
    val joinCreator: Boolean = false,
    val targetScore: Int? = null,
    val useTargetScore: Boolean = false,
    val allowDraw: Boolean = false,
    val winPoints: Int = 3,
    val drawPoints: Int = 1,
    val lossPoints: Int = 0,
    val cappottoEnabled: Boolean = true,
    val cappottoBonusPoints: Int = 2,
    val matchFormat: String = "POINTS",
    val winByTwo: Boolean = false,
    val matchCreationMode: String = "FREE",
    val calendarGenerationMode: String = "ROUNDS",
    val homeAndAway: Boolean = false,
    val tournamentFormat: String? = null // SINGLE_ELIMINATION, GROUPS_THEN_SINGLE_ELIMINATION
)

@Serializable
data class CompetitionTemplateResponse(
    val sportId: Long,
    val sportName: String,
    val competitionName: String,
    val type: String,
    val rankingMode: String,
    val matchType: String,
    val competitionRankingType: String,
    val matchFormat: String,
    val targetScore: Int? = null,
    val useTargetScore: Boolean = false,
    val allowDraw: Boolean = false,
    val winPoints: Int = 3,
    val drawPoints: Int = 0,
    val lossPoints: Int = 0,
    val cappottoEnabled: Boolean = false,
    val cappottoBonusPoints: Int = 0,
    val homeAndAway: Boolean = false,
    val calendarGenerationMode: String = "ROUNDS"
)

// --- Sport ---

@Serializable
data class SportResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    val active: Boolean,
    val rankingEnabled: Boolean,
    val rankingType: String,
    val sportMode: String
)

// --- Squadre ---

@Serializable
data class TeamResponse( // Renamed from Team
    val id: Long,
    val seasonId: Long? = null,
    val competitionId: Long? = null,
    val name: String,
    val playerAId: Long? = null,
    val playerAUsername: String? = null,
    val playerBId: Long? = null,
    val playerBUsername: String? = null,
    val inviteCode: String? = null,
    val rating: Int? = null,
    val points: Int? = null,
    val goalsFor: Int? = null,
    val goalsAgainst: Int? = null,
    val matchesPlayed: Int? = null,
    val cappottiGiven: Int? = null,
    val cappottiReceived: Int? = null
)

@Serializable
data class CreateTeamRequest(
    val name: String,
    val playerAId: Long? = null,
    val playerBId: Long? = null
)

@Serializable
data class TeamMemberResponse(
    val id: Long? = null,
    val teamId: Long? = null,
    val userId: Long? = null,
    val username: String? = null,
    val role: String? = null // OWNER, PLAYER
)

@Serializable
data class JoinTeamResponse(
    val teamId: Long,
    val teamName: String,
    val role: String
)

// --- Partite ---

@Serializable
data class MatchResponse( // Renamed from Match
    val id: Long,
    val seasonId: Long? = null,
    val competitionId: Long? = null,
    val type: String? = null,
    val teamAId: Long? = null,
    val teamAName: String? = null,
    val teamBId: Long? = null,
    val teamBName: String? = null,
    val competitionName: String? = null,
    val leagueName: String? = null,
    val scoreA: Int? = null,
    val scoreB: Int? = null,
    val playedAt: String? = null,
    val cappotto: Boolean? = null,
    val cappottoBonusApplied: Int? = null,
    val teamARatingBefore: Int? = null,
    val teamARatingAfter: Int? = null,
    val teamARatingDelta: Int? = null,
    val teamBRatingBefore: Int? = null,
    val teamBRatingAfter: Int? = null,
    val teamBRatingDelta: Int? = null,
    val roundNumber: Int? = null,
    val bracketRound: Int? = null,
    val bracketPosition: Int? = null,
    val nextMatchId: Long? = null,
    val nextMatchSlot: String? = null,
    val groupId: Long? = null,
    val groupName: String? = null,
    val knockoutStage: Boolean = false,
    val resultInsertable: Boolean = true
)

@Serializable
data class CreateDoubleMatchRequest( // Renamed from CreateMatchRequest
    val teamAId: Long,
    val teamBId: Long,
    val scoreA: Int,
    val scoreB: Int
)

@Serializable
data class UpdateMatchResultRequest(
    val scoreA: Int,
    val scoreB: Int
)

@Serializable
data class GenerateRandomMatchesRequest( // Renamed from RandomMatchesRequest
    val matchesCount: Int
)

@Serializable
data class MatchCommentResponse(
    val id: Long,
    val matchId: Long,
    val userId: Long,
    val username: String,
    val message: String,
    val createdAt: String
)

@Serializable
data class CompetitionCommentResponse(
    val id: Long,
    val competitionId: Long,
    val userId: Long,
    val username: String,
    val message: String,
    val createdAt: String
)

@Serializable
data class CreateMatchCommentRequest(
    val message: String
)

@Serializable
data class CreateCompetitionCommentRequest(
    val message: String
)

@Serializable
data class LeagueCommentResponse(
    val id: Long,
    val leagueId: Long,
    val userId: Long,
    val username: String,
    val message: String,
    val createdAt: String
)

@Serializable
data class CreateLeagueCommentRequest(
    val message: String
)

// --- Classifiche & Statistiche ---

@Serializable
data class PlayerRankingResponse( // Renamed from PlayerRanking
    val userId: Long,
    val username: String? = null,
    val rating: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val matchesPlayed: Int,
    val cappottiGiven: Int,
    val cappottiReceived: Int
)

@Serializable
data class TeamRankingResponse( // Renamed from TeamRanking
    val teamId: Long,
    val teamName: String,
    val playerAId: Long? = null,
    val playerAUsername: String? = null,
    val playerBId: Long? = null,
    val playerBUsername: String? = null,
    val rating: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val matchesPlayed: Int,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val points: Int = 0,
    val goalDifference: Int = 0,
    val cappottiGiven: Int? = null,
    val cappottiReceived: Int? = null
)

@Serializable
data class RatingHistoryResponse( // Renamed from RatingHistoryEntry
    val matchId: Long? = null,
    val playedAt: String? = null,
    val ratingBefore: Int? = null,
    val ratingAfter: Int? = null,
    val delta: Int? = null
)

@Serializable
data class TeamStatsResponse(
    val teamId: Long,
    val teamName: String,
    val rating: Int,
    val matchesPlayed: Int,
    val wins: Int,
    val draws: Int = 0,
    val losses: Int,
    val points: Int = 0,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val cappottiGiven: Int,
    val cappottiReceived: Int,
    val winPercentage: Double,
    val averageGoalsFor: Double,
    val averageGoalsAgainst: Double
)

@Serializable
data class HeadToHeadResponse(
    val teamAId: Long,
    val teamAName: String,
    val teamBId: Long,
    val teamBName: String,
    val totalMatches: Int,
    val teamAWins: Int,
    val teamBWins: Int,
    val teamAGoals: Int,
    val teamBGoals: Int,
    val teamACappotti: Int,
    val teamBCappotti: Int,
    val lastMatch: MatchResponse? = null,
    val matches: List<MatchResponse> = emptyList()
)

@Serializable
data class PlayerStatsResponse(
    val userId: Long,
    val username: String,
    val rating: Int,
    val matchesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val cappottiGiven: Int,
    val cappottiReceived: Int,
    val winPercentage: Double,
    val averageGoalsFor: Double,
    val averageGoalsAgainst: Double
)

@Serializable
data class PlayerPartnerStatsResponse(
    val partnerId: Long,
    val partnerUsername: String,
    val matchesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val ratingDelta: Int,
    val winPercentage: Double
)

@Serializable
data class TrophyResponse(
    val competitionId: Long,
    val competitionName: String,
    val competitionType: String? = null,
    val sportName: String? = null,
    val matchType: String? = null,
    val winnerTeamId: Long? = null,
    val winnerTeamName: String? = null,
    val winnerUserId: Long? = null,
    val winnerUserName: String? = null,
    val closedAt: String? = null
)

@Serializable
data class PlayerProfileResponse(
    val userId: Long,
    val username: String,
    val matchesPlayed: Int,
    val wins: Int,
    val draws: Int = 0,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val winPercentage: Double,
    val trophies: List<TrophyResponse> = emptyList()
)

@Serializable
data class TeamProfileResponse(
    val teamId: Long,
    val teamName: String,
    val matchesPlayed: Int,
    val wins: Int,
    val draws: Int = 0,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val winPercentage: Double,
    val trophies: List<TrophyResponse> = emptyList()
)
