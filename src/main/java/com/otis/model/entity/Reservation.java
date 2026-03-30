package com.otis.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Reservation entity with two-phase commit support.
 * 
 * Status values:
 * - 'pending': Reservation created, awaiting payment confirmation
 * - 'confirmed': Payment completed, seats are booked
 * - 'cancelled': Reservation was cancelled by user
 * - 'expired': Hold timeout exceeded, seats released
 */
public record Reservation(
    UUID id,
    UUID eventId,
    String customerName,
    Integer seatCount,
    String status,
    Instant expiresAt
) {
    /**
     * Check if reservation is pending payment
     */
    public boolean isPending() {
        return "pending".equals(status);
    }

    /**
     * Check if reservation is confirmed
     */
    public boolean isConfirmed() {
        return "confirmed".equals(status);
    }

    /**
     * Check if reservation has expired
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * Check if reservation can be cancelled
     */
    public boolean canBeCancelled() {
        return isPending() || isExpired();
    }
}
