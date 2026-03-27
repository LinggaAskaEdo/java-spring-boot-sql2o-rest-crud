package com.otis.model.entity;

import java.util.UUID;

public record Reservation(UUID id, UUID eventId, String customerName, Integer seatCount) {
}
