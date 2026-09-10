package com.vigilai.service;

import com.vigilai.dto.ProofSubmissionResponse;
import com.vigilai.entity.*;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.ProjectRepository;
import com.vigilai.repository.ProofSubmissionRepository;
import com.vigilai.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Orchestrates "Proof of Execution": receive a photo, ask Gemini to
 * verify it, record only the verdict, auto-complete the task if
 * verified. The photo bytes never touch disk and are discarded the
 * moment this method returns — see GeminiVisionService for why.
 */
@Service
public class ProofService {

    private static final long MAX_PROOF_SIZE_BYTES = 8L * 1024 * 1024; // 8MB, images only

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ProofSubmissionRepository proofSubmissionRepository;
    private final GeminiVisionService geminiVisionService;
    private final WorkspaceService workspaceService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final AccountabilityService accountabilityService;
    private final WorkspaceEventPublisher eventPublisher;

    public ProofService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            ProofSubmissionRepository proofSubmissionRepository,
            GeminiVisionService geminiVisionService,
            WorkspaceService workspaceService,
            ActivityLogService activityLogService,
            NotificationService notificationService,
            AccountabilityService accountabilityService,
            WorkspaceEventPublisher eventPublisher
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.proofSubmissionRepository = proofSubmissionRepository;
        this.geminiVisionService = geminiVisionService;
        this.workspaceService = workspaceService;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
        this.accountabilityService = accountabilityService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProofSubmissionResponse submitProof(Long taskId, Long userId, MultipartFile photo) {
        if (photo.isEmpty()) {
            throw ApiException.badRequest("Please attach a photo as proof");
        }
        if (photo.getSize() > MAX_PROOF_SIZE_BYTES) {
            throw ApiException.badRequest("Photo exceeds the 8MB limit");
        }
        String contentType = photo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ApiException.badRequest("Proof must be an image file");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> ApiException.notFound("Task not found"));
        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> ApiException.notFound("Project not found"));
        workspaceService.assertMember(project.getWorkspaceId(), userId);

        byte[] imageBytes;
        try {
            imageBytes = photo.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Could not read the uploaded photo", e);
        }

        // The one and only place the photo bytes are used — never written to disk.
        GeminiVisionService.ValidationResult result = geminiVisionService.validate(
                imageBytes, contentType, task.getTitle(), task.getDescription());
        // imageBytes goes out of scope here and is eligible for garbage collection —
        // as close to "the photo is deleted" as a JVM in-memory array gets.

        ProofSubmission submission = ProofSubmission.builder()
                .taskId(taskId)
                .submittedBy(userId)
                .verified(result.verified())
                .aiReason(result.reason())
                .build();
        submission = proofSubmissionRepository.save(submission);

        if (result.verified()) {
            task.setStatus(TaskStatus.DONE);
            taskRepository.save(task);

            accountabilityService.recordVerifiedCompletion(userId, project.getWorkspaceId());

            activityLogService.log(project.getWorkspaceId(), userId, "PROOF_VERIFIED", "TASK", taskId,
                    "AI verified proof for \"" + task.getTitle() + "\": " + result.reason());

            if (task.getAssigneeId() != null) {
                notificationService.notify(task.getAssigneeId(), NotificationType.TASK_STATUS_CHANGED,
                        "Proof accepted — \"" + task.getTitle() + "\" marked done", "TASK", taskId);
            }

            eventPublisher.publish(project.getWorkspaceId(), "PROOF_VERIFIED", java.util.Map.of(
                    "taskId", taskId,
                    "verified", true,
                    "reason", result.reason()
            ));
        } else {
            activityLogService.log(project.getWorkspaceId(), userId, "PROOF_REJECTED", "TASK", taskId,
                    "AI rejected proof for \"" + task.getTitle() + "\": " + result.reason());

            eventPublisher.publish(project.getWorkspaceId(), "PROOF_REJECTED", java.util.Map.of(
                    "taskId", taskId,
                    "verified", false,
                    "reason", result.reason()
            ));
        }

        return ProofSubmissionResponse.builder()
                .id(submission.getId())
                .taskId(taskId)
                .verified(submission.isVerified())
                .aiReason(submission.getAiReason())
                .taskStatus(task.getStatus().name())
                .submittedAt(submission.getSubmittedAt())
                .build();
    }
}