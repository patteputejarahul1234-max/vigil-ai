package com.vigilai.controller;

import com.vigilai.dto.ProofSubmissionResponse;
import com.vigilai.security.CurrentUser;
import com.vigilai.service.ProofService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "Proof of Execution", description = "AI-verified task completion via photo (Stage 4)")
public class ProofController {

    private final ProofService proofService;
    private final CurrentUser currentUser;

    public ProofController(ProofService proofService, CurrentUser currentUser) {
        this.proofService = proofService;
        this.currentUser = currentUser;
    }

    @PostMapping(value = "/api/tasks/{taskId}/proof", consumes = "multipart/form-data")
    public ProofSubmissionResponse submitProof(Authentication auth, @PathVariable Long taskId,
    @RequestParam("photo") MultipartFile photo) {
        return proofService.submitProof(taskId, currentUser.idOf(auth), photo);
    }
}