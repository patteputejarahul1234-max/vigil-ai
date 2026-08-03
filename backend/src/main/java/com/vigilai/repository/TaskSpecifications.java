package com.vigilai.repository;

import com.vigilai.dto.TaskSearchRequest;
import com.vigilai.entity.Project;
import com.vigilai.entity.Task;
import com.vigilai.entity.TaskPriority;
import com.vigilai.entity.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a dynamic JPA Specification from whichever TaskSearchRequest
 * fields are populated — this is the "Search & Filters" feature.
 * Any field left null is simply skipped, so one endpoint covers
 * "just show me my tasks", "high priority tasks due this week", etc.
 */
public class TaskSpecifications {

    private TaskSpecifications() {}

    public static Specification<Task> fromRequest(TaskSearchRequest req, List<Long> projectIdsInWorkspace) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (req.getProjectId() != null) {
                predicates.add(cb.equal(root.get("projectId"), req.getProjectId()));
            } else if (projectIdsInWorkspace != null) {
                predicates.add(root.get("projectId").in(projectIdsInWorkspace));
            }

            if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), TaskStatus.valueOf(req.getStatus().toUpperCase())));
            }

            if (req.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), TaskPriority.valueOf(req.getPriority().toUpperCase())));
            }

            if (req.getAssigneeId() != null) {
                predicates.add(cb.equal(root.get("assigneeId"), req.getAssigneeId()));
            }

            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }

            if (req.getDueBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), req.getDueBefore()));
            }

            if (req.getDueAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), req.getDueAfter()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
