package com.vigilai.service;

import com.vigilai.dto.TaskRequest;
import com.vigilai.dto.TaskResponse;
import com.vigilai.dto.TaskSearchRequest;
import com.vigilai.entity.*;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.ProjectRepository;
import com.vigilai.repository.TaskAttachmentRepository;
import com.vigilai.repository.TaskRepository;
import com.vigilai.repository.TaskSpecifications;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final TaskAttachmentRepository attachmentRepository;
    private final WorkspaceService workspaceService;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final CacheManager cacheManager;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            TaskAttachmentRepository attachmentRepository,
            WorkspaceService workspaceService,
            NotificationService notificationService,
            ActivityLogService activityLogService,
            CacheManager cacheManager
    ) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.attachmentRepository = attachmentRepository;
        this.workspaceService = workspaceService;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
        this.cacheManager = cacheManager;
    }

    /** Analytics numbers change on every task write, so evict rather than let the TTL alone handle it. */
    private void evictAnalytics(Long workspaceId) {
        var cache = cacheManager.getCache("analytics");
        if (cache != null) cache.evict(workspaceId);
    }

    @Transactional
    public TaskResponse create(Long projectId, Long userId, TaskRequest request) {
        Project project = findProjectOrThrow(projectId);
        workspaceService.assertMember(project.getWorkspaceId(), userId);

        Task task = Task.builder()
                .projectId(projectId)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? parseStatus(request.getStatus()) : TaskStatus.TODO)
                .priority(request.getPriority() != null ? parsePriority(request.getPriority()) : TaskPriority.MEDIUM)
                .assigneeId(request.getAssigneeId())
                .dueDate(request.getDueDate())
                .createdBy(userId)
                .build();
        task = taskRepository.save(task);

        if (task.getAssigneeId() != null) {
            notificationService.notify(task.getAssigneeId(), NotificationType.TASK_ASSIGNED,
                    "You were assigned to \"" + task.getTitle() + "\"", "TASK", task.getId());
        }

        activityLogService.log(project.getWorkspaceId(), userId, "TASK_CREATED", "TASK", task.getId(),
                "Created task \"" + task.getTitle() + "\"");
        evictAnalytics(project.getWorkspaceId());

        return toResponse(task);
    }

    public List<TaskResponse> getForProject(Long projectId, Long userId) {
        Project project = findProjectOrThrow(projectId);
        workspaceService.assertMember(project.getWorkspaceId(), userId);
        return taskRepository.findByProjectId(projectId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TaskResponse getById(Long taskId, Long userId) {
        Task task = findOrThrow(taskId);
        Project project = findProjectOrThrow(task.getProjectId());
        workspaceService.assertMember(project.getWorkspaceId(), userId);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse update(Long taskId, Long userId, TaskRequest request) {
        Task task = findOrThrow(taskId);
        Project project = findProjectOrThrow(task.getProjectId());
        workspaceService.assertMember(project.getWorkspaceId(), userId);

        TaskStatus previousStatus = task.getStatus();
        Long previousAssignee = task.getAssigneeId();

        if (request.getTitle() != null && !request.getTitle().isBlank()) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStatus() != null) task.setStatus(parseStatus(request.getStatus()));
        if (request.getPriority() != null) task.setPriority(parsePriority(request.getPriority()));
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        if (request.getAssigneeId() != null) task.setAssigneeId(request.getAssigneeId());

        task = taskRepository.save(task);

        if (task.getStatus() != previousStatus) {
            activityLogService.log(project.getWorkspaceId(), userId, "TASK_STATUS_CHANGED", "TASK", task.getId(),
                    task.getTitle() + ": " + previousStatus + " -> " + task.getStatus());

            if (task.getAssigneeId() != null) {
                notificationService.notify(task.getAssigneeId(), NotificationType.TASK_STATUS_CHANGED,
                        "\"" + task.getTitle() + "\" moved to " + task.getStatus(), "TASK", task.getId());
            }
        }

        if (task.getAssigneeId() != null && !task.getAssigneeId().equals(previousAssignee)) {
            notificationService.notify(task.getAssigneeId(), NotificationType.TASK_ASSIGNED,
                    "You were assigned to \"" + task.getTitle() + "\"", "TASK", task.getId());
        }

        evictAnalytics(project.getWorkspaceId());
        return toResponse(task);
    }

    @Transactional
    public void delete(Long taskId, Long userId) {
        Task task = findOrThrow(taskId);
        Project project = findProjectOrThrow(task.getProjectId());
        workspaceService.assertMember(project.getWorkspaceId(), userId);

        taskRepository.delete(task);
        activityLogService.log(project.getWorkspaceId(), userId, "TASK_DELETED", "TASK", taskId,
                "Deleted task \"" + task.getTitle() + "\"");
        evictAnalytics(project.getWorkspaceId());
    }

    /** Powers the Search & Filters feature — dynamic query across any combination of fields. */
    public List<TaskResponse> search(TaskSearchRequest request, Long userId) {
        List<Long> projectIdsInWorkspace = null;

        if (request.getProjectId() != null) {
            Project project = findProjectOrThrow(request.getProjectId());
            workspaceService.assertMember(project.getWorkspaceId(), userId);
        } else if (request.getWorkspaceId() != null) {
            workspaceService.assertMember(request.getWorkspaceId(), userId);
            projectIdsInWorkspace = projectRepository.findByWorkspaceId(request.getWorkspaceId())
                    .stream().map(Project::getId).collect(Collectors.toList());
        } else {
            throw ApiException.badRequest("Provide either projectId or workspaceId to search tasks");
        }

        return taskRepository.findAll(TaskSpecifications.fromRequest(request, projectIdsInWorkspace))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // --- helpers ---

    Task findOrThrow(Long taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> ApiException.notFound("Task not found"));
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> ApiException.notFound("Project not found"));
    }

    private TaskStatus parseStatus(String raw) {
        try {
            return TaskStatus.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid task status: " + raw);
        }
    }

    private TaskPriority parsePriority(String raw) {
        try {
            return TaskPriority.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid task priority: " + raw);
        }
    }

    private TaskResponse toResponse(Task task) {
        int attachmentCount = attachmentRepository.findByTaskId(task.getId()).size();
        return TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assigneeId(task.getAssigneeId())
                .createdBy(task.getCreatedBy())
                .dueDate(task.getDueDate())
                .attachmentCount(attachmentCount)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
