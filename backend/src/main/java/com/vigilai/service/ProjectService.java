package com.vigilai.service;

import com.vigilai.dto.ProjectRequest;
import com.vigilai.dto.ProjectResponse;
import com.vigilai.entity.Project;
import com.vigilai.entity.ProjectStatus;
import com.vigilai.entity.TaskStatus;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.ProjectRepository;
import com.vigilai.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceService workspaceService;
    private final ActivityLogService activityLogService;

    public ProjectService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            WorkspaceService workspaceService,
            ActivityLogService activityLogService
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.workspaceService = workspaceService;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public ProjectResponse create(Long workspaceId, Long userId, ProjectRequest request) {
        workspaceService.assertMember(workspaceId, userId);

        Project project = Project.builder()
                .workspaceId(workspaceId)
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(userId)
                .build();
        project = projectRepository.save(project);

        activityLogService.log(workspaceId, userId, "PROJECT_CREATED", "PROJECT", project.getId(),
                "Created project \"" + project.getName() + "\"");

        return toResponse(project);
    }

    public List<ProjectResponse> getForWorkspace(Long workspaceId, Long userId) {
        workspaceService.assertMember(workspaceId, userId);
        return projectRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getById(Long projectId, Long userId) {
        Project project = findOrThrow(projectId);
        workspaceService.assertMember(project.getWorkspaceId(), userId);
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse update(Long projectId, Long userId, ProjectRequest request) {
        Project project = findOrThrow(projectId);
        workspaceService.assertMember(project.getWorkspaceId(), userId);

        if (request.getName() != null && !request.getName().isBlank()) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            project.setStatus(parseStatus(request.getStatus()));
        }

        project = projectRepository.save(project);

        activityLogService.log(project.getWorkspaceId(), userId, "PROJECT_UPDATED", "PROJECT", project.getId(),
                "Updated project \"" + project.getName() + "\"");

        return toResponse(project);
    }

    @Transactional
    public void delete(Long projectId, Long userId) {
        Project project = findOrThrow(projectId);
        workspaceService.assertMember(project.getWorkspaceId(), userId);
        projectRepository.delete(project);

        activityLogService.log(project.getWorkspaceId(), userId, "PROJECT_DELETED", "PROJECT", projectId,
                "Deleted project \"" + project.getName() + "\"");
    }

    Project findOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> ApiException.notFound("Project not found"));
    }

    private ProjectStatus parseStatus(String raw) {
        try {
            return ProjectStatus.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid project status: " + raw);
        }
    }

    private ProjectResponse toResponse(Project project) {
        var tasks = taskRepository.findByProjectId(project.getId());
        long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();

        return ProjectResponse.builder()
                .id(project.getId())
                .workspaceId(project.getWorkspaceId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .createdBy(project.getCreatedBy())
                .taskCount(tasks.size())
                .completedTaskCount((int) completed)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
