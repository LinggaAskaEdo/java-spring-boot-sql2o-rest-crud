package com.otis.model.dto;

import java.util.UUID;

public record ReservationRequest(UUID eventId, String customerName, int seatCount) {
}
