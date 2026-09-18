-- V3: Fix project table (add missing columns) and remove Hibernate-auto-created orphan tables
-- Idempotent where possible using MySQL 8 syntax

-- 1. Add 'status' column to project if it doesn't already exist
ALTER TABLE project
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) NOT NULL DEFAULT 'PLANNED';

-- 2. Add 'created_by' column to project if it doesn't already exist
ALTER TABLE project
    ADD COLUMN IF NOT EXISTS created_by BIGINT NOT NULL DEFAULT 0;

-- 3. Drop the orphan 'read' column that V2 mistakenly added
--    (V1 already had 'is_read'; entity now correctly maps to is_read)
ALTER TABLE app_notification DROP COLUMN IF EXISTS `read`;

-- 4. Remove the incorrectly-named tables that Hibernate may have auto-created
--    (entities are now corrected to map to 'workspace' and 'workspace_member')
DROP TABLE IF EXISTS workspace_members;
DROP TABLE IF EXISTS workspaces;

