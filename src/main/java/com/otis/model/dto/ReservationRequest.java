package com.otis.model.dto;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Reservation request DTO with validation.
 * Supports two-phase reservation: reserve (hold) → confirm (book).
 */
public record ReservationRequest(
		@NotNull(message = "Event ID is required")
		UUID eventId,

		@NotBlank(message = "Customer name is required")
		String customerName,

		@NotNull(message = "Seat count is required")
		@Min(value = 1, message = "Seat count must be at least 1")
		Integer seatCount,
		
		@Min(value = 1, message = "Maximum seats per reservation is 6")
		@Max(value = 6, message = "Maximum seats per reservation is 6")
		Integer maxSeats) {
	
	public ReservationRequest {
		// Default maxSeats to 6 if not specified
		if (maxSeats == null) {
			maxSeats = 6;
		}
	}
}
