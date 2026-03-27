-- Migration: create_table_seats
-- Created : 2026-03-27 10:00:01 UTC

-- Seats table
CREATE TABLE IF NOT EXISTS seats (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID_V7()),
    event_id CHAR(36) NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    reserved BOOLEAN DEFAULT FALSE,
    reservation_id CHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    INDEX idx_event_id (event_id),
    INDEX idx_reserved (reserved)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
