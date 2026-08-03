package com.vigilai.controller;

import com.vigilai.dto.TaskRequest;
import com.vigilai.dto.TaskResponse;
import com.vigilai.dto.TaskSearchRequest;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Tasks", description = "Tasks belong to a project; includes search & filters")
public class TaskController {

    private final TaskService taskService;
    private final CurrentUser currentUser;

    public TaskController(TaskService taskService, CurrentUser currentUser) {
        this.taskService = taskService;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    public TaskResponse create(Authentication auth, @PathVariable Long projectId,
                                @Valid @RequestBody TaskRequest request) {
        return taskService.create(projectId, currentUser.idOf(auth), request);
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    public List<TaskResponse> list(Authentication auth, @PathVariable Long projectId) {
        return taskService.getForProject(projectId, currentUser.idOf(auth));
    }

    @GetMapping("/api/tasks/{taskId}")
    public TaskResponse getOne(Authentication auth, @PathVariable Long taskId) {
        return taskService.getById(taskId, currentUser.idOf(auth));
    }

    @PutMapping("/api/tasks/{taskId}")
    public TaskResponse update(Authentication auth, @PathVariable Long taskId,
                                @RequestBody TaskRequest request) {
        return taskService.update(taskId, currentUser.idOf(auth), request);
    }

    @DeleteMapping("/api/tasks/{taskId}")
    public void delete(Authentication auth, @PathVariable Long taskId) {
        taskService.delete(taskId, currentUser.idOf(auth));
    }

    /**
     * Search & Filters — pass any combination: projectId or workspaceId (required, pick one),
     * status, priority, assigneeId, keyword, dueBefore, dueAfter (all optional).
     * e.g. GET /api/tasks/search?workspaceId=1&status=TODO&priority=HIGH&keyword=water
     */
    @GetMapping("/api/tasks/search")
    public List<TaskResponse> search(Authentication auth, TaskSearchRequest request) {
        return taskService.search(request, currentUser.idOf(auth));
    }
}
