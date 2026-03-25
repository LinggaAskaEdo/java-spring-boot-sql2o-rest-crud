-- Migration: create_table_tutorials
-- Created : 2026-03-25 06:10:06 UTC

-- Tutorials table
CREATE TABLE IF NOT EXISTS tutorials (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID_V7()),
    title VARCHAR(50) DEFAULT NULL,
    description VARCHAR(100) NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Sample data: Tutorials
INSERT INTO tutorials (id, title, description, published) VALUES
    ('018e0000-0000-7000-8002-000000000001', 'a', 'aaa', TRUE);