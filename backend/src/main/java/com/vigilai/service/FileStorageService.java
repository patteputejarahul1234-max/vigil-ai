package com.vigilai.service;

import com.vigilai.dto.TaskAttachmentResponse;
import com.vigilai.entity.Project;
import com.vigilai.entity.Task;
import com.vigilai.entity.TaskAttachment;
import com.vigilai.exception.ApiException;
import com.vigilai.repository.ProjectRepository;
import com.vigilai.repository.TaskAttachmentRepository;
import com.vigilai.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Local-disk file storage for Stage 2. Swappable later for S3/Cloud Storage —
 * everything above this class only talks to save()/loadAsResource(), so the
 * migration to Stage 3 cloud deployment is a one-file change.
 */
@Service
public class FileStorageService {

    private final Path rootLocation;
    private final TaskAttachmentRepository attachmentRepository;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceService workspaceService;
    private final ActivityLogService activityLogService;

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    public FileStorageService(
            @Value("${app.upload-dir:uploads}") String uploadDir,
            TaskAttachmentRepository attachmentRepository,
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            WorkspaceService workspaceService,
            ActivityLogService activityLogService
    ) throws IOException {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.rootLocation);
        this.attachmentRepository = attachmentRepository;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.workspaceService = workspaceService;
        this.activityLogService = activityLogService;
    }

    @Transactional
    public TaskAttachmentResponse uploadToTask(Long taskId, Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("Cannot upload an empty file");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw ApiException.badRequest("File exceeds the 10MB limit");
        }

        Task task = taskRepository.findById(taskId).orElseThrow(() -> ApiException.notFound("Task not found"));
        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> ApiException.notFound("Project not found"));
        workspaceService.assertMember(project.getWorkspaceId(), userId);

        String originalName = Path.of(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename())
                .getFileName().toString();
        String storedName = UUID.randomUUID() + "_" + originalName;

        try {
            Path target = rootLocation.resolve(storedName).normalize();
            if (!target.getParent().equals(rootLocation)) {
                throw ApiException.badRequest("Invalid file path");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }

        TaskAttachment attachment = TaskAttachment.builder()
                .taskId(taskId)
                .fileName(originalName)
                .storedPath(storedName)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .uploadedBy(userId)
                .build();
        attachment = attachmentRepository.save(attachment);

        activityLogService.log(project.getWorkspaceId(), userId, "FILE_UPLOADED", "TASK", taskId,
                "Uploaded " + originalName + " to \"" + task.getTitle() + "\"");

        return toResponse(attachment);
    }

    public List<TaskAttachmentResponse> getForTask(Long taskId, Long userId) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> ApiException.notFound("Task not found"));
        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> ApiException.notFound("Project not found"));
        workspaceService.assertMember(project.getWorkspaceId(), userId);

        return attachmentRepository.findByTaskId(taskId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Resource loadAsResource(Long attachmentId, Long userId) {
        TaskAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> ApiException.notFound("Attachment not found"));
        Task task = taskRepository.findById(attachment.getTaskId())
                .orElseThrow(() -> ApiException.notFound("Task not found"));
        Project project = projectRepository.findById(task.getProjectId())
                .orElseThrow(() -> ApiException.notFound("Project not found"));
        workspaceService.assertMember(project.getWorkspaceId(), userId);

        try {
            Path file = rootLocation.resolve(attachment.getStoredPath()).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw ApiException.notFound("File not found on disk");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw ApiException.notFound("File not found");
        }
    }

    public String fileNameFor(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .map(TaskAttachment::getFileName)
                .orElse("download");
    }

    private TaskAttachmentResponse toResponse(TaskAttachment a) {
        return TaskAttachmentResponse.builder()
                .id(a.getId())
                .taskId(a.getTaskId())
                .fileName(a.getFileName())
                .downloadUrl("/api/files/" + a.getId() + "/download")
                .contentType(a.getContentType())
                .sizeBytes(a.getSizeBytes())
                .uploadedBy(a.getUploadedBy())
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}
