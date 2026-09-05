package com.vigilai.service;

import com.vigilai.dto.SmartSuggestionResponse;
import com.vigilai.entity.Project;
import com.vigilai.entity.ProofSubmission;
import com.vigilai.entity.Task;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.ProjectRepository;
import com.vigilai.repository.ProofSubmissionRepository;
import com.vigilai.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure data analysis, no AI call — looks at WHEN this task has actually
 * been completed successfully in the past (verified proof submissions)
 * and suggests the hour of day that works best for the user, based on
 * their own real behavior rather than a generic default reminder time.
 */
@Service
public class SmartSuggestionService {

    private static final int MIN_DATA_POINTS = 3;

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProofSubmissionRepository proofSubmissionRepository;
    private final WorkspaceService workspaceService;

    public SmartSuggestionService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            ProofSubmissionRepository proofSubmissionRepository,
            WorkspaceService workspaceService
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.proofSubmissionRepository = proofSubmissionRepository;
        this.workspaceService = workspaceService;
    }

    public SmartSuggestionResponse getForTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> ApiException.notFound("Task not found"));
        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> ApiException.notFound("Project not found"));
        workspaceService.assertMember(project.getWorkspaceId(), userId);

        List<ProofSubmission> verifiedSubmissions = proofSubmissionRepository
                .findByTaskIdOrderBySubmittedAtDesc(taskId)
                .stream()
                .filter(ProofSubmission::isVerified)
                .collect(Collectors.toList());

        if (verifiedSubmissions.size() < MIN_DATA_POINTS) {
            return SmartSuggestionResponse.builder()
                    .hasEnoughData(false)
                    .message("Complete this task a few more times with verified proof, "
                            + "and Vigil AI will learn the best time to remind you.")
                    .completedCount(verifiedSubmissions.size())
                    .build();
        }

        // Find the hour of day (0-23) this task is most often completed at
        Map<Integer, Long> hourCounts = verifiedSubmissions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getSubmittedAt().atZone(ZoneOffset.UTC).getHour(),
                        Collectors.counting()
                ));

        int bestHour = hourCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(9);

        return SmartSuggestionResponse.builder()
                .hasEnoughData(true)
                .message(describeHour(bestHour, task.getTitle()))
                .suggestedHourOfDay(bestHour)
                .completedCount(verifiedSubmissions.size())
                .build();
    }

    private String describeHour(int hour, String taskTitle) {
        String period = hour < 12 ? "morning" : hour < 17 ? "afternoon" : "evening";
        int displayHour = hour % 12 == 0 ? 12 : hour % 12;
        return "You tend to complete \"" + taskTitle + "\" most often around "
                + displayHour + (hour < 12 ? "AM" : "PM") + " (" + period
                + ") — that's a good time to schedule your reminder.";
    }
}