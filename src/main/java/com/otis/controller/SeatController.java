package com.otis.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.otis.model.dto.ReservationRequest;
import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Reservation;
import com.otis.model.entity.Seat;
import com.otis.service.SeatService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin(origins = "${api.cors.allowed-origins:*}")
@RestController
@RequestMapping("/api/v1")
@Validated
public class SeatController {
	private final SeatService seatService;
	private final int maxPageSize;
	private final int defaultPageSize;

	public SeatController(SeatService seatService,
			@Value("${api.pagination.max-page-size:100}") int maxPageSize,
			@Value("${api.pagination.default-page-size:10}") int defaultPageSize) {
		this.seatService = seatService;
		this.maxPageSize = maxPageSize;
		this.defaultPageSize = defaultPageSize;
	}

	@GetMapping("/events/{eventId}/seats")
	public ResponseEntity<PageResponse<Seat>> getSeatsByEvent(
			@PathVariable UUID eventId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "") String size) {

		// Apply pagination limits
		int effectiveSize = parseSize(size, defaultPageSize);

		return ResponseEntity.ok(seatService.findByEventId(page, effectiveSize, eventId));
	}

	@GetMapping("/events/{eventId}/seats/available")
	public ResponseEntity<Integer> getAvailableSeatsCount(@PathVariable UUID eventId) {
		int count = seatService.getAvailableSeatsCount(eventId);
		return ResponseEntity.ok(count);
	}

	@PostMapping("/events/{eventId}/reserve")
	public ResponseEntity<Reservation> reserveSeats(
			@PathVariable UUID eventId,
			@Valid @RequestBody ReservationRequest request) {
		log.info("Reserve seats request: eventId={}, customerName={}, seatCount={}",
				eventId, request.customerName(), request.seatCount());

		Reservation reservation = seatService.reserveSeats(eventId, request.customerName(), request.seatCount());

		return ResponseEntity.ok(reservation);
	}

	@PostMapping("/reservations/{reservationId}/confirm")
	public ResponseEntity<Reservation> confirmReservation(@PathVariable UUID reservationId) {
		log.info("Confirm reservation request: reservationId={}", reservationId);

		boolean confirmed = seatService.confirmReservation(reservationId);

		if (!confirmed) {
			return ResponseEntity.badRequest().build();
		}

		return ResponseEntity.ok().build();
	}

	@PostMapping("/reservations/{reservationId}/cancel")
	public ResponseEntity<Void> cancelReservation(@PathVariable UUID reservationId) {
		boolean released = seatService.cancelReservation(reservationId);
		if (released) {
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.notFound().build();
	}

	private int parseSize(String size, int defaultSize) {
		if (size == null || size.isBlank()) {
			return defaultSize;
		}
		try {
			int parsed = Integer.parseInt(size);
			return (parsed <= 0) ? defaultSize : Math.min(parsed, maxPageSize);
		} catch (NumberFormatException e) {
			return defaultSize;
		}
	}
}
