package com.otis.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Seat entity with status-based reservation management.
 * Supports two-phase reservation: reserve (hold) → confirm (book).
 * 
 * Status values:
 * - 'available': Seat is available for reservation
 * - 'reserved': Seat is temporarily held (awaiting payment confirmation)
 * - 'booked': Seat is confirmed and paid for
 */
public record Seat(
    UUID id,
    UUID eventId,
    String seatNumber,
    String status,
    UUID heldBy,
    Instant heldUntil,
    Integer version,
    Boolean reserved,
    UUID reservationId
) {
    /**
     * Check if seat is available for reservation
     */
    public boolean isAvailable() {
        return "available".equals(status);
    }

    /**
     * Check if seat is currently reserved (held)
     */
    public boolean isReserved() {
        return "reserved".equals(status);
    }

    /**
     * Check if seat is booked (confirmed)
     */
    public boolean isBooked() {
        return "booked".equals(status);
    }

    /**
     * Check if hold has expired
     */
    public boolean isHoldExpired() {
        if (heldUntil == null) {
            return false;
        }
        return Instant.now().isAfter(heldUntil);
    }
}
