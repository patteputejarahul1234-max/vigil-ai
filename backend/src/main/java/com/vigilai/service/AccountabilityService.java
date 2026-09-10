package com.vigilai.service;

import com.vigilai.dto.LeaderboardEntryDto;
import com.vigilai.entity.AccountabilityStats;
import com.vigilai.entity.User;
import com.vigilai.entity.WorkspaceMember;
import com.vigilai.repository.AccountabilityStatsRepository;
import com.vigilai.repository.UserRepository;
import com.vigilai.repository.WorkspaceMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AccountabilityService {

    private final AccountabilityStatsRepository statsRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;
    private final WorkspaceEventPublisher eventPublisher;

    public AccountabilityService(
            AccountabilityStatsRepository statsRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            WorkspaceService workspaceService,
            WorkspaceEventPublisher eventPublisher
    ) {
        this.statsRepository = statsRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.workspaceService = workspaceService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AccountabilityStats recordVerifiedCompletion(Long userId, Long workspaceId) {
        AccountabilityStats stats = statsRepository.findByUserId(userId)
                .orElseGet(() -> AccountabilityStats.builder().userId(userId).build());

        LocalDate today = LocalDate.now();
        LocalDate last = stats.getLastCompletedDate();

        if (last == null) {
            stats.setCurrentStreak(1);
        } else if (last.equals(today.minusDays(1))) {
            stats.setCurrentStreak(stats.getCurrentStreak() + 1);
        } else if (!last.equals(today)) {
            // Gap was more than 1 day, reset streak to 1
            stats.setCurrentStreak(1);
        }

        stats.setLongestStreak(Math.max(stats.getLongestStreak(), stats.getCurrentStreak()));
        stats.setLastCompletedDate(today);
        stats.setTotalVerifiedTasks(stats.getTotalVerifiedTasks() + 1);

        // Score: base 50 + 5 per streak day + 2 per verified completion, capped at 100
        int score = Math.min(100, 50 + (stats.getCurrentStreak() * 5) + (stats.getTotalVerifiedTasks() * 2));
        stats.setAccountabilityScore(score);

        AccountabilityStats saved = statsRepository.save(stats);

        if (workspaceId != null) {
            eventPublisher.publish(workspaceId, "LEADERBOARD_UPDATED", Map.of(
                    "userId", userId,
                    "streak", saved.getCurrentStreak(),
                    "score", saved.getAccountabilityScore()
            ));
        }

        return saved;
    }

    public AccountabilityStats getStats(Long userId) {
        return statsRepository.findByUserId(userId)
                .orElseGet(() -> AccountabilityStats.builder()
                        .userId(userId)
                        .currentStreak(0)
                        .longestStreak(0)
                        .totalVerifiedTasks(0)
                        .accountabilityScore(100)
                        .build());
    }

    public List<LeaderboardEntryDto> getWorkspaceLeaderboard(Long workspaceId, Long currentUserId) {
        workspaceService.assertMember(workspaceId, currentUserId);

        List<WorkspaceMember> members = memberRepository.findByWorkspaceId(workspaceId);
        List<Long> userIds = members.stream().map(WorkspaceMember::getUserId).toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, AccountabilityStats> statsMap = statsRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(AccountabilityStats::getUserId, Function.identity()));

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        for (Long uid : userIds) {
            User user = userMap.get(uid);
            if (user == null) continue;

            AccountabilityStats stat = statsMap.getOrDefault(uid, AccountabilityStats.builder()
                    .userId(uid)
                    .currentStreak(0)
                    .longestStreak(0)
                    .totalVerifiedTasks(0)
                    .accountabilityScore(100)
                    .build());

            entries.add(new LeaderboardEntryDto(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getAvatarUrl(),
                    stat.getCurrentStreak(),
                    stat.getLongestStreak(),
                    stat.getTotalVerifiedTasks(),
                    stat.getAccountabilityScore(),
                    0,
                    ""
            ));
        }

        // Sort by accountability score desc, then current streak desc, then verified tasks desc
        entries.sort(Comparator
                .comparingInt(LeaderboardEntryDto::accountabilityScore).reversed()
                .thenComparing(Comparator.comparingInt(LeaderboardEntryDto::currentStreak).reversed())
                .thenComparing(Comparator.comparingInt(LeaderboardEntryDto::totalVerifiedTasks).reversed()));

        List<LeaderboardEntryDto> ranked = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntryDto e = entries.get(i);
            int rank = i + 1;
            String badge = determineBadge(rank, e.currentStreak(), e.totalVerifiedTasks());
            ranked.add(new LeaderboardEntryDto(
                    e.userId(),
                    e.fullName(),
                    e.email(),
                    e.avatarUrl(),
                    e.currentStreak(),
                    e.longestStreak(),
                    e.totalVerifiedTasks(),
                    e.accountabilityScore(),
                    rank,
                    badge
            ));
        }

        return ranked;
    }

    private String determineBadge(int rank, int streak, int verified) {
        if (rank == 1 && verified > 0) return "🏆 Champion";
        if (streak >= 7) return "🔥 Unstoppable";
        if (streak >= 3) return "⚡ Consistent";
        if (verified >= 3) return "⭐ Achiever";
        return "🌱 Starter";
    }
}
