package com.vigilai.repository;

import com.vigilai.entity.ProofSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProofSubmissionRepository extends JpaRepository<ProofSubmission, Long> {
    List<ProofSubmission> findByTaskIdOrderBySubmittedAtDesc(Long taskId);
}