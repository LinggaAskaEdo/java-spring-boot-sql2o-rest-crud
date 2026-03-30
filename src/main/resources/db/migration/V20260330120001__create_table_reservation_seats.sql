-- Migration: create_table_reservation_seats
-- Created : 2026-03-30 12:00:01 UTC
-- Purpose: Junction table for reservation-seat relationship

CREATE TABLE IF NOT EXISTS reservation_seats (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID_V7()),
    reservation_id CHAR(36) NOT NULL,
    seat_id CHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE CASCADE,
    UNIQUE KEY uk_reservation_seat (reservation_id, seat_id),
    INDEX idx_reservation_id (reservation_id),
    INDEX idx_seat_id (seat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
