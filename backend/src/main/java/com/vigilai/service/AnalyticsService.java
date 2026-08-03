package com.vigilai.service;

import com.vigilai.dto.AnalyticsResponse;
import com.vigilai.entity.Project;
import com.vigilai.entity.Task;
import com.vigilai.entity.TaskStatus;
import com.vigilai.repository.ProjectRepository;
import com.vigilai.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AnalyticsService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceService workspaceService;

    public AnalyticsService(ProjectRepository projectRepository, TaskRepository taskRepository, WorkspaceService workspaceService) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.workspaceService = workspaceService;
    }

    public AnalyticsResponse getForWorkspace(Long workspaceId, Long userId) {
        workspaceService.assertMember(workspaceId, userId);

        List<Project> projects = projectRepository.findByWorkspaceId(workspaceId);
        List<Task> tasks = projects.stream()
                .flatMap(p -> taskRepository.findByProjectId(p.getId()).stream())
                .collect(Collectors.toList());

        long total = tasks.size();
        long completed = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        long overdue = tasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(Instant.now()) && t.getStatus() != TaskStatus.DONE)
                .count();

        Map<String, Long> byStatus = Stream.of(TaskStatus.values())
                .collect(Collectors.toMap(Enum::name, s -> tasks.stream().filter(t -> t.getStatus() == s).count()));

        Map<String, Long> byPriority = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getPriority().name(), Collectors.counting()));

        return AnalyticsResponse.builder()
                .totalProjects(projects.size())
                .totalTasks(total)
                .completedTasks(completed)
                .completionRate(total == 0 ? 0.0 : Math.round((completed * 1000.0 / total)) / 10.0)
                .tasksByStatus(byStatus)
                .tasksByPriority(byPriority)
                .overdueTasks(overdue)
                .build();
    }
}
