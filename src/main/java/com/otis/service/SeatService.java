package com.otis.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.exception.BadRequestException;
import com.otis.exception.ResourceNotFoundException;
import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Reservation;
import com.otis.model.entity.ReservationSeat;
import com.otis.model.entity.Seat;
import com.otis.repository.ReservationRepository;
import com.otis.repository.ReservationSeatRepository;
import com.otis.repository.SeatRepository;
import com.otis.util.UuidUtils;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SeatService {
	private final SeatRepository seatRepository;
	private final ReservationRepository reservationRepository;
	private final ReservationSeatRepository reservationSeatRepository;

	/**
	 * Hold duration in seconds (10 minutes as per race.txt best practices)
	 */
	private static final int HOLD_DURATION_SECONDS = 600;

	public SeatService(SeatRepository seatRepository,
			ReservationRepository reservationRepository,
			ReservationSeatRepository reservationSeatRepository) {
		this.seatRepository = seatRepository;
		this.reservationRepository = reservationRepository;
		this.reservationSeatRepository = reservationSeatRepository;
	}

	@Bulkhead(name = "database")
	@Retry(name = "database")
	public PageResponse<Seat> findByEventId(int page, int size, UUID eventId) {
		return seatRepository.findByEventId(page, size, eventId);
	}

	/**
	 * Reserve seats with proper deadlock prevention and partial rollback.
	 * Uses Sql2o transaction management instead of @Transactional.
	 *
	 * Key design decisions from race.txt:
	 * 1. Lock rows in ascending ID order to prevent deadlocks
	 * 2. Use FOR NO KEY UPDATE instead of FOR UPDATE for better concurrency
	 * 3. Validate-then-update in one pass within transaction
	 * 4. Partial rollback if not all seats can be reserved
	 * 5. Two-phase: reserve (hold) → confirm (book)
	 *
	 * @param eventId      the event to reserve seats for
	 * @param customerName customer name
	 * @param seatCount    number of seats to reserve
	 * @return reservation with pending status
	 * @throws ResourceNotFoundException if no seats available
	 * @throws BadRequestException       if not enough seats available
	 */
	@Bulkhead(name = "database")
	public Reservation reserveSeats(UUID eventId, String customerName, int seatCount) {
		return seatRepository.executeInTransaction(() -> {
			UUID reservationId = UuidUtils.randomUuidV7();
			Instant heldUntil = Instant.now().plusSeconds(HOLD_DURATION_SECONDS);

			// Step 1: Find and lock available seats (locks in ascending ID order)
			List<Seat> lockedSeats = seatRepository.findAndLockAvailableSeats(eventId, seatCount);

			if (lockedSeats.isEmpty()) {
				log.warn("No seats available for event {}", eventId);
				throw new ResourceNotFoundException("No seats available for the requested event");
			}

			if (lockedSeats.size() < seatCount) {
				log.warn("Not enough seats available. Requested: {}, Available: {}", seatCount, lockedSeats.size());
				throw new BadRequestException(
						"Only " + lockedSeats.size() + " seats available, requested " + seatCount);
			}

			// Step 2: Extract seat IDs (already sorted by ID from query)
			List<UUID> seatIds = lockedSeats.stream().map(Seat::id).toList();

			// Step 3: Create reservation record
			Reservation reservation = new Reservation(
					reservationId,
					eventId,
					customerName,
					seatCount,
					"pending",
					heldUntil);
			reservationRepository.create(reservation);

			// Step 4: Mark seats as reserved with optimistic locking
			int reserved = seatRepository.markSeatsAsReserved(seatIds, reservationId);

			// Step 5: Handle partial failure - rollback any seats that were reserved
			if (reserved != seatCount) {
				log.error("Partial reservation failure. Expected: {}, Reserved: {}. Rolling back...",
						seatCount, reserved);

				// Release the seats that were reserved
				List<UUID> reservedSeatIds = lockedSeats.stream()
						.limit(reserved)
						.map(Seat::id)
						.toList();
				seatRepository.releaseSeatsByIds(reservedSeatIds);

				// Delete the reservation
				reservationRepository.deleteById(reservationId);

				throw new IllegalStateException(
						"Failed to reserve all requested seats. Only " + reserved + " of " + seatCount + " were reserved.");
			}

			// Step 6: Create reservation-seat relationships
			reservationSeatRepository.createReservationSeats(reservationId, seatIds);

			log.info("Successfully reserved {} seats for customer {} (reservationId: {}, heldUntil: {})",
					reserved, customerName, reservationId, heldUntil);
			return reservation;
		});
	}

	/**
	 * Confirm a pending reservation (complete the two-phase commit).
	 * Uses Sql2o transaction management.
	 *
	 * @param reservationId the reservation to confirm
	 * @return true if confirmed, false if not found or not pending
	 */
	@Bulkhead(name = "database")
	@Retry(name = "database")
	public boolean confirmReservation(UUID reservationId) {
		return seatRepository.executeInTransaction(() -> {
			Reservation reservation = reservationRepository.findById(reservationId);

			if (reservation == null) {
				log.warn("Reservation {} not found for confirmation", reservationId);
				return false;
			}

			if (!reservation.isPending()) {
				log.warn("Reservation {} is not pending (status: {}), cannot confirm",
						reservationId, reservation.status());
				return false;
			}

			if (reservation.isExpired()) {
				log.warn("Reservation {} has expired, cannot confirm", reservationId);
				reservationRepository.expire(reservationId);
				return false;
			}

			// Get seats for this reservation
			List<ReservationSeat> reservationSeats =
					reservationSeatRepository.findByReservationId(reservationId);
			List<UUID> seatIds = reservationSeats.stream()
					.map(ReservationSeat::seatId)
					.toList();

			// Mark seats as booked
			int confirmed = seatRepository.confirmSeats(seatIds, reservationId);

			if (confirmed != reservation.seatCount()) {
				log.error("Partial confirmation failure. Expected: {}, Confirmed: {}",
						reservation.seatCount(), confirmed);
				return false;
			}

			// Update reservation status
			reservationRepository.confirm(reservationId);

			log.info("Confirmed reservation {} with {} seats", reservationId, confirmed);
			return true;
		});
	}

	/**
	 * Cancel a reservation and release seats.
	 * Uses Sql2o transaction management.
	 *
	 * @param reservationId the reservation to cancel
	 * @return true if cancelled, false if not found
	 */
	@Bulkhead(name = "database")
	@Retry(name = "database")
	public boolean cancelReservation(UUID reservationId) {
		return seatRepository.executeInTransaction(() -> {
			Reservation reservation = reservationRepository.findById(reservationId);

			if (reservation == null) {
				log.warn("Reservation {} not found for cancellation", reservationId);
				return false;
			}

			if (!reservation.canBeCancelled()) {
				log.warn("Reservation {} cannot be cancelled (status: {})",
						reservationId, reservation.status());
				return false;
			}

			// Get seats for this reservation
			List<ReservationSeat> reservationSeats =
					reservationSeatRepository.findByReservationId(reservationId);
			List<UUID> seatIds = reservationSeats.stream()
					.map(ReservationSeat::seatId)
					.toList();

			// Release seats
			seatRepository.releaseSeatsByIds(seatIds);

			// Delete reservation-seat relationships
			reservationSeatRepository.deleteByReservationId(reservationId);

			// Update reservation status
			reservationRepository.cancel(reservationId);

			log.info("Cancelled reservation {}", reservationId);
			return true;
		});
	}

	/**
	 * Release expired reservations (background job).
	 * Uses Sql2o transaction management.
	 *
	 * @return number of seats released
	 */
	public int releaseExpiredReservations() {
		try {
			int released = seatRepository.releaseExpiredReservations();
			if (released > 0) {
				log.info("Released {} expired reservation seats", released);
			}
			return released;
		} catch (Exception e) {
			log.error("Error releasing expired reservations: {}", e.getMessage());
			throw e;
		}
	}

	/**
	 * Get available seats count for an event.
	 */
	@Bulkhead(name = "database")
	@Retry(name = "database")
	public int getAvailableSeatsCount(UUID eventId) {
		return seatRepository.countAvailableSeats(eventId);
	}
}
