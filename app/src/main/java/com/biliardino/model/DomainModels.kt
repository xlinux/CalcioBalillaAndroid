package com.biliardino.model

import kotlinx.serialization.Serializable

// --- Auth ---

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val name: String, // Changed from username
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String? = null, // Changed from accessToken
    val userId: Long,
    val name: String? = null, // Changed from username
    val email: String? = null
)

// --- Leghe ---

@Serializable
data class LeagueResponse( // Renamed from League to match Swagger
    val id: Long,
    val name: String,
    val description: String,
    val inviteCode: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class CreateLeagueRequest(
    val name: String,
    val description: String,
    val imageUrl: String? = null
)

@Serializable
data class JoinLeagueRequest(
    val inviteCode: String
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
    val cappottiReceived: Int
)

// --- Stagioni ---

@Serializable
data class SeasonResponse( // Renamed from Season
    val id: Long,
    val leagueId: Long,
    val name: String,
    val startDate: String,
    val endDate: String,
    val active: Boolean? = null,
    val targetScore: Int,
    val cappottoEnabled: Boolean,
    val cappottoBonus: Int,
    val allowJoinAfterStart: Boolean,
    val allowMatchesAfterEnd: Boolean
)

@Serializable
data class CreateSeasonRequest(
    val name: String,
    val startDate: String,
    val endDate: String,
    val targetScore: Int,
    val cappottoEnabled: Boolean,
    val cappottoBonus: Int,
    val allowJoinAfterStart: Boolean,
    val allowMatchesAfterEnd: Boolean
)

// --- Squadre ---

@Serializable
data class TeamResponse( // Renamed from Team
    val id: Long,
    val seasonId: Long? = null,
    val name: String,
    val playerAId: Long,
    val playerAUsername: String? = null,
    val playerBId: Long,
    val playerBUsername: String? = null,
    val rating: Int? = null,
    val goalsFor: Int? = null,
    val goalsAgainst: Int? = null,
    val matchesPlayed: Int? = null,
    val cappottiGiven: Int? = null,
    val cappottiReceived: Int? = null
)

@Serializable
data class CreateTeamRequest(
    val name: String,
    val playerAId: Long,
    val playerBId: Long
)

// --- Partite ---

@Serializable
data class MatchResponse( // Renamed from Match
    val id: Long,
    val seasonId: Long? = null,
    val type: String? = null,
    val teamAId: Long,
    val teamAName: String? = null,
    val teamBId: Long,
    val teamBName: String? = null,
    val scoreA: Int,
    val scoreB: Int,
    val playedAt: String? = null,
    val cappotto: Boolean? = null,
    val cappottoBonusApplied: Int? = null,
    val teamARatingBefore: Int? = null,
    val teamARatingAfter: Int? = null,
    val teamARatingDelta: Int? = null,
    val teamBRatingBefore: Int? = null,
    val teamBRatingAfter: Int? = null,
    val teamBRatingDelta: Int? = null
)

@Serializable
data class CreateDoubleMatchRequest( // Renamed from CreateMatchRequest
    val teamAId: Long,
    val teamBId: Long,
    val scoreA: Int,
    val scoreB: Int
)

@Serializable
data class GenerateRandomMatchesRequest( // Renamed from RandomMatchesRequest
    val matchesCount: Int
)

// --- Classifiche & Statistiche ---

@Serializable
data class PlayerRankingResponse( // Renamed from PlayerRanking
    val userId: Long,
    val username: String,
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
    val playerAUsername: String,
    val playerBUsername: String,
    val rating: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val matchesPlayed: Int,
    val cappottiGiven: Int,
    val cappottiReceived: Int
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
