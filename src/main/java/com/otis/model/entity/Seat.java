package com.otis.model.entity;

import java.util.UUID;

public record Seat(UUID id, UUID eventId, String seatNumber, Boolean reserved, UUID reservationId) {
}
