-- Migration: create_table_reservations
-- Created : 2026-03-27 10:00:02 UTC

-- Reservations table
CREATE TABLE IF NOT EXISTS reservations (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID_V7()),
    event_id CHAR(36) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    seat_count INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    INDEX idx_event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
