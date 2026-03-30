package com.otis.model.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Junction entity linking reservations to seats.
 * Supports many-to-many relationship between reservations and seats.
 */
public record ReservationSeat(
    UUID id,
    UUID reservationId,
    UUID seatId,
    Instant createdAt
) {
}
