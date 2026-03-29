package com.otis.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.otis.model.dto.ReservationRequest;
import com.otis.model.entity.Reservation;
import com.otis.service.ReservationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class ReservationController {
	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping("/reservations")
	public ResponseEntity<Reservation> createReservation(@RequestBody ReservationRequest request) {
		Reservation reservation = reservationService.create(request);
		return ResponseEntity.ok(reservation);
	}

	@GetMapping("/reservations/{id}")
	public ResponseEntity<Reservation> getReservationById(@PathVariable UUID id) {
		Reservation reservation = reservationService.findById(id);
		if (reservation == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(reservation);
	}
}
