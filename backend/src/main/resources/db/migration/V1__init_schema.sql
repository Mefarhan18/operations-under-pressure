-- Newtonite Operations: Initial Schema Migration (V1)
-- Engineered for reliability, auditability, and historical integrity

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Teams table
CREATE TABLE IF NOT EXISTS teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Memberships table (user-team association with role)
CREATE TABLE IF NOT EXISTS memberships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_membership_user_team UNIQUE (user_id, team_id),
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_membership_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. WorkItems table (core operational entity with optimistic locking)
CREATE TABLE IF NOT EXISTS work_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    type VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    team_id BIGINT NOT NULL,
    assignee_id BIGINT NULL,
    created_by_id BIGINT NOT NULL,
    due_date DATE NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_work_item_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT,
    CONSTRAINT fk_work_item_assignee FOREIGN KEY (assignee_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_work_item_creator FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_work_items_team ON work_items (team_id);
CREATE INDEX idx_work_items_assignee ON work_items (assignee_id);
CREATE INDEX idx_work_items_status ON work_items (status);
CREATE INDEX idx_work_items_priority ON work_items (priority);
CREATE INDEX idx_work_items_type ON work_items (type);

-- 5. ActivityEvents table (immutable historical record of operational changes)
CREATE TABLE IF NOT EXISTS activity_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_item_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_event_work_item FOREIGN KEY (work_item_id) REFERENCES work_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_activity_event_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_activity_events_work_item ON activity_events (work_item_id);
CREATE INDEX idx_activity_events_actor ON activity_events (actor_id);
CREATE INDEX idx_activity_events_created ON activity_events (created_at);

-- 6. Comments table (collaboration on work items)
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_item_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comment_work_item FOREIGN KEY (work_item_id) REFERENCES work_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_comments_work_item ON comments (work_item_id);
CREATE INDEX idx_comments_author ON comments (author_id);
