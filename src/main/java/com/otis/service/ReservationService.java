package com.otis.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.dto.ReservationRequest;
import com.otis.model.entity.Reservation;
import com.otis.repository.ReservationRepository;
import com.otis.util.UuidUtils;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReservationService {
	private final ReservationRepository reservationRepository;

	public ReservationService(ReservationRepository reservationRepository) {
		this.reservationRepository = reservationRepository;
	}

	@Bulkhead(name = "database")
	@Retry(name = "database")
	public Reservation create(ReservationRequest request) {
		UUID id = UuidUtils.randomUuidV7();
		Reservation reservation = new Reservation(
				id,
				request.eventId(),
				request.customerName(),
				request.seatCount(),
				"pending",
				java.time.Instant.now().plusSeconds(600));

		reservationRepository.create(reservation);
		return reservation;
	}

	@Bulkhead(name = "database")
	@Retry(name = "database")
	public Reservation findById(UUID id) {
		return reservationRepository.findById(id);
	}

	@Bulkhead(name = "database")
	@Retry(name = "database")
	public boolean confirm(UUID id) {
		return reservationRepository.confirm(id);
	}

	@Bulkhead(name = "database")
	@Retry(name = "database")
	public boolean cancel(UUID id) {
		return reservationRepository.cancel(id);
	}
}
