package com.vigilai.service;

import com.vigilai.dto.AssistantQueryResponse;
import com.vigilai.entity.Project;
import com.vigilai.entity.Task;
import com.vigilai.entity.TaskStatus;
import com.vigilai.repository.ProjectRepository;
import com.vigilai.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Answers natural-language questions about a workspace's own real
 * tasks — "what's overdue", "what should I focus on today" — by
 * building a plain-text summary of actual task data and handing it
 * to Gemini as context. This grounds every answer in real data
 * instead of letting the model guess.
 */
@Service
public class AiAssistantService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceService workspaceService;
    private final GeminiTextService geminiTextService;

    public AiAssistantService(
            ProjectRepository projectRepository,
            TaskRepository taskRepository,
            WorkspaceService workspaceService,
            GeminiTextService geminiTextService
    ) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.workspaceService = workspaceService;
        this.geminiTextService = geminiTextService;
    }

    public AssistantQueryResponse ask(Long workspaceId, Long userId, String question) {
        workspaceService.assertMember(workspaceId, userId);

        List<Project> projects = projectRepository.findByWorkspaceId(workspaceId);
        List<Task> tasks = projects.stream()
                .flatMap(p -> taskRepository.findByProjectId(p.getId()).stream())
                .collect(Collectors.toList());

        String context = buildContext(tasks);
        String prompt = "You are a helpful assistant inside Vigil AI, a task accountability app. "
                + "Answer the user's question using ONLY the task data below — don't invent tasks "
                + "that aren't listed. Be concise and direct, a few sentences at most.\n\n"
                + "Current tasks:\n" + context
                + "\n\nUser's question: " + question;

        String answer = geminiTextService.ask(prompt);
        return AssistantQueryResponse.builder().answer(answer).build();
    }

    private String buildContext(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return "(No tasks exist yet in this workspace.)";
        }

        Instant now = Instant.now();
        return tasks.stream()
                .map(t -> {
                    String overdue = (t.getDueDate() != null && t.getDueDate().isBefore(now)
                            && t.getStatus() != TaskStatus.DONE) ? " [OVERDUE]" : "";
                    return "- \"" + t.getTitle() + "\" — status: " + t.getStatus()
                            + ", priority: " + t.getPriority() + overdue;
                })
                .collect(Collectors.joining("\n"));
    }
}