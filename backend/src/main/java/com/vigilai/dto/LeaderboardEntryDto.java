package com.vigilai.dto;

public record LeaderboardEntryDto(
        Long userId,
        String fullName,
        String email,
        String avatarUrl,
        int currentStreak,
        int longestStreak,
        int totalVerifiedTasks,
        int accountabilityScore,
        int rank,
        String badge
) {}
