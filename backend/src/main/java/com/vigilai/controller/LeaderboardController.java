package com.vigilai.controller;

import com.vigilai.dto.LeaderboardEntryDto;
import com.vigilai.entity.AccountabilityStats;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.AccountabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Leaderboard & Streaks", description = "Accountability scores, streaks, and workspace rankings")
public class LeaderboardController {

    private final AccountabilityService accountabilityService;
    private final CurrentUser currentUser;

    public LeaderboardController(AccountabilityService accountabilityService, CurrentUser currentUser) {
        this.accountabilityService = accountabilityService;
        this.currentUser = currentUser;
    }

    @GetMapping("/workspaces/{workspaceId}/leaderboard")
    @Operation(summary = "Get workspace leaderboard", description = "Returns members ranked by accountability score, streak, and verified tasks")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @PathVariable Long workspaceId,
            Authentication auth
    ) {
        return ResponseEntity.ok(accountabilityService.getWorkspaceLeaderboard(workspaceId, currentUser.idOf(auth)));
    }

    @GetMapping("/users/me/stats")
    @Operation(summary = "Get current user streak and accountability stats")
    public ResponseEntity<AccountabilityStats> getMyStats(Authentication auth) {
        return ResponseEntity.ok(accountabilityService.getStats(currentUser.idOf(auth)));
    }
}
