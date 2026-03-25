-- Migration: create_table_tutorial_details
-- Created : 2026-03-25 06:10:38 UTC

-- Tutorial details table
CREATE TABLE IF NOT EXISTS tutorial_details (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID_V7()),
    tutorial_id CHAR(36) NOT NULL,
    created_on DATETIME NOT NULL,
    created_by VARCHAR(26) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT td_tutorial_fk FOREIGN KEY (tutorial_id) REFERENCES tutorials(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Sample data: Tutorial Details
INSERT INTO tutorial_details (id, tutorial_id, created_on, created_by) VALUES
    ('018e0000-0000-7000-8003-000000000001', '018e0000-0000-7000-8002-000000000001', '2023-09-03 00:00:00', 'test a');