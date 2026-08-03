package com.vigilai.service;

import com.vigilai.dto.ActivityLogResponse;
import com.vigilai.entity.ActivityLog;
import com.vigilai.entity.User;
import com.vigilai.repository.ActivityLogRepository;
import com.vigilai.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Append-only audit trail. Every meaningful action in a workspace
 * (task created, status changed, member invited, etc.) writes one row
 * here — this is what "Activity Logs" in Stage 2 actually is.
 */
@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository, UserRepository userRepository) {
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    public void log(Long workspaceId, Long actorId, String action, String entityType, Long entityId, String details) {
        ActivityLog entry = ActivityLog.builder()
                .workspaceId(workspaceId)
                .actorId(actorId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();
        activityLogRepository.save(entry);
    }

    public List<ActivityLogResponse> getForWorkspace(Long workspaceId) {
        List<ActivityLog> logs = activityLogRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);

        Map<Long, String> namesById = userRepository.findAllById(
                logs.stream().map(ActivityLog::getActorId).distinct().collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(User::getId, User::getFullName));

        return logs.stream()
                .map(l -> ActivityLogResponse.builder()
                        .id(l.getId())
                        .actorId(l.getActorId())
                        .actorName(namesById.getOrDefault(l.getActorId(), "Unknown"))
                        .action(l.getAction())
                        .entityType(l.getEntityType())
                        .entityId(l.getEntityId())
                        .details(l.getDetails())
                        .createdAt(l.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
