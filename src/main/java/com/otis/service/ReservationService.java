package com.otis.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.dto.ReservationRequest;
import com.otis.model.entity.Reservation;
import com.otis.repository.ReservationRepository;
import com.otis.util.BulkheadUtils;
import com.otis.util.UuidUtils;

import io.github.resilience4j.bulkhead.Bulkhead;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReservationService {
	private final ReservationRepository reservationRepository;
	private final Bulkhead bulkhead;

	public ReservationService(ReservationRepository reservationRepository, Bulkhead databaseBulkhead) {
		this.reservationRepository = reservationRepository;
		this.bulkhead = databaseBulkhead;
	}

	public Reservation create(ReservationRequest request) {
		UUID id = UuidUtils.randomUuidV7();
		Reservation reservation = new Reservation(id, request.eventId(), request.customerName(), request.seatCount());

		BulkheadUtils.withBulkhead(bulkhead,
				() -> {
					reservationRepository.create(reservation);
					return null;
				}, "createReservation");

		return reservation;
	}

	public Reservation findById(UUID id) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> reservationRepository.findById(id), "findReservationById");
	}
}
