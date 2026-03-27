package com.otis.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Reservation;
import com.otis.model.entity.Seat;
import com.otis.service.SeatService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class SeatController {
	private final SeatService seatService;

	public SeatController(SeatService seatService) {
		this.seatService = seatService;
	}

	@GetMapping("/events/{eventId}/seats")
	public ResponseEntity<PageResponse<Seat>> getSeatsByEvent(
			@PathVariable UUID eventId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(seatService.findByEventId(page, size, eventId));
	}

	@PostMapping("/events/{eventId}/reserve")
	public ResponseEntity<Reservation> reserveSeats(
			@PathVariable UUID eventId,
			@RequestBody ReservationRequest request) {
		log.info("Reserve seats request: eventId={}, customerName={}, seatCount={}",
				eventId, request.customerName(), request.seatCount());

		Reservation reservation = seatService.reserveSeats(eventId, request.customerName(), request.seatCount());

		if (reservation == null) {
			return ResponseEntity.badRequest().build();
		}

		return ResponseEntity.ok(reservation);
	}

	@PostMapping("/reservations/{reservationId}/cancel")
	public ResponseEntity<Void> cancelReservation(@PathVariable UUID reservationId) {
		boolean released = seatService.cancelReservation(reservationId);
		if (released) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}

	public record ReservationRequest(String customerName, int seatCount) {
	}
}
