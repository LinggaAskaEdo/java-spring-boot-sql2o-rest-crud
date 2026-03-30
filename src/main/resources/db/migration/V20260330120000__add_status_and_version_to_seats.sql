-- Migration: add_status_and_version_to_seats
-- Created : 2026-03-30 12:00:00 UTC
-- Purpose: Add status, held_by, held_until, and version columns for proper seat reservation

-- Add new columns to seats table
ALTER TABLE seats 
    ADD COLUMN status ENUM('available', 'reserved', 'booked') DEFAULT 'available' AFTER reserved,
    ADD COLUMN held_by CHAR(36) AFTER status,
    ADD COLUMN held_until TIMESTAMP NULL AFTER held_by,
    ADD COLUMN version INT DEFAULT 0 AFTER held_until;

-- Migrate existing data: set status based on reserved flag
UPDATE seats 
SET status = CASE 
    WHEN reserved = TRUE AND reservation_id IS NOT NULL THEN 'reserved'
    ELSE 'available'
END;

-- Create index for event and status combination (faster availability checks)
CREATE INDEX idx_seats_event_status ON seats(event_id, status);

-- Create index for held_until (expiration cleanup)
CREATE INDEX idx_seats_held_until ON seats(held_until);

-- Create index for version (optimistic locking)
CREATE INDEX idx_seats_version ON seats(id, version);
