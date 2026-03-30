-- Migration: add_status_to_reservations
-- Created : 2026-03-30 12:00:02 UTC
-- Purpose: Add status and expires_at for two-phase reservation

ALTER TABLE reservations
    ADD COLUMN status ENUM('pending', 'confirmed', 'cancelled', 'expired') DEFAULT 'pending' AFTER seat_count,
    ADD COLUMN expires_at TIMESTAMP NULL AFTER status;

-- Set existing reservations to confirmed
UPDATE reservations SET status = 'confirmed' WHERE id IS NOT NULL;

-- Create index for expiration cleanup job
CREATE INDEX idx_reservations_expires_at ON reservations(expires_at);

-- Create index for status filtering
CREATE INDEX idx_reservations_status ON reservations(status);
