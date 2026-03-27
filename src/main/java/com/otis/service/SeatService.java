package com.otis.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Reservation;
import com.otis.model.entity.Seat;
import com.otis.repository.ReservationRepository;
import com.otis.repository.SeatRepository;
import com.otis.util.BulkheadUtils;

import io.github.resilience4j.bulkhead.Bulkhead;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SeatService {
	private final SeatRepository seatRepository;
	private final ReservationRepository reservationRepository;
	private final Bulkhead bulkhead;

	public SeatService(SeatRepository seatRepository, ReservationRepository reservationRepository,
			Bulkhead databaseBulkhead) {
		this.seatRepository = seatRepository;
		this.reservationRepository = reservationRepository;
		this.bulkhead = databaseBulkhead;
	}

	public PageResponse<Seat> findByEventId(int page, int size, UUID eventId) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> seatRepository.findByEventId(page, size, eventId), "findByEventId");
	}

	public Reservation reserveSeats(UUID eventId, String customerName, int seatCount) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> reserveSeatsInternal(eventId, customerName, seatCount), "reserveSeats");
	}

	private Reservation reserveSeatsInternal(UUID eventId, String customerName, int seatCount) {
		UUID reservationId = UUID.randomUUID();

		List<Seat> lockedSeats = seatRepository.findAndLockAvailableSeats(eventId, seatCount);

		if (lockedSeats.size() < seatCount) {
			log.warn("Not enough seats available. Requested: {}, Available: {}", seatCount, lockedSeats.size());
			return null;
		}

		List<UUID> seatIds = lockedSeats.stream().map(Seat::getId).toList();
		int reserved = seatRepository.reserveSeatsInTransaction(seatIds, reservationId);

		if (reserved != seatCount) {
			seatRepository.releaseSeats(reservationId);
			log.warn("Failed to reserve all seats. Reserved: {}", reserved);
			return null;
		}

		Reservation reservation = Reservation.builder()
				.id(reservationId)
				.eventId(eventId)
				.customerName(customerName)
				.seatCount(reserved)
				.build();

		reservationRepository.create(reservation);

		log.info("Successfully reserved {} seats for customer {}", reserved, customerName);
		return reservation;
	}

	public boolean cancelReservation(UUID reservationId) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> seatRepository.releaseSeats(reservationId), "cancelReservation");
	}
}
