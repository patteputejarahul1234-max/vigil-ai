package com.vigilai.controller;

import com.vigilai.dto.TaskAttachmentResponse;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.FileStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Tag(name = "Files", description = "File upload/download for task attachments (proof-of-execution photos, etc.)")
public class FileController {

    private final FileStorageService fileStorageService;
    private final CurrentUser currentUser;

    public FileController(FileStorageService fileStorageService, CurrentUser currentUser) {
        this.fileStorageService = fileStorageService;
        this.currentUser = currentUser;
    }

    @PostMapping(value = "/api/tasks/{taskId}/attachments", consumes = "multipart/form-data")
    public TaskAttachmentResponse upload(Authentication auth, @PathVariable Long taskId,
                                          @RequestParam("file") MultipartFile file) {
        return fileStorageService.uploadToTask(taskId, currentUser.idOf(auth), file);
    }

    @GetMapping("/api/tasks/{taskId}/attachments")
    public List<TaskAttachmentResponse> list(Authentication auth, @PathVariable Long taskId) {
        return fileStorageService.getForTask(taskId, currentUser.idOf(auth));
    }

    @GetMapping("/api/files/{attachmentId}/download")
    public ResponseEntity<Resource> download(Authentication auth, @PathVariable Long attachmentId) {
        Resource resource = fileStorageService.loadAsResource(attachmentId, currentUser.idOf(auth));
        String fileName = fileStorageService.fileNameFor(attachmentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }
}
