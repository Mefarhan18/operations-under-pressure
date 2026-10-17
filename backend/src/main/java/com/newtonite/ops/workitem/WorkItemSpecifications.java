package com.newtonite.ops.workitem;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class WorkItemSpecifications {

    public static Specification<WorkItem> withFilters(
            List<Long> allowedTeamIds,
            Long specificTeamId,
            WorkItemStatus status,
            WorkItemPriority priority,
            Long assigneeId,
            String search
    ) {
        return (root, query, cb) -> {
            // Eagerly fetch associations on row queries to eliminate N+1 queries (avoiding on count queries)
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("team", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("assignee", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("createdBy", jakarta.persistence.criteria.JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            // 1. Team isolation: Restrict to allowed teams or specific team
            if (specificTeamId != null) {
                predicates.add(cb.equal(root.get("team").get("id"), specificTeamId));
            } else if (allowedTeamIds != null && !allowedTeamIds.isEmpty()) {
                predicates.add(root.get("team").get("id").in(allowedTeamIds));
            } else {
                // If user has no teams, match nothing
                return cb.disjunction();
            }

            // 2. Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 3. Priority filter
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            // 4. Assignee filter
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }

            // 5. Full text substring search across title and description
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
