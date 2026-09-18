-- V3: Safe schema update for project table and foreign keys
SET FOREIGN_KEY_CHECKS = 0;

-- Drop orphaned duplicate tables if present
DROP TABLE IF EXISTS workspace_members;

SET FOREIGN_KEY_CHECKS = 1;