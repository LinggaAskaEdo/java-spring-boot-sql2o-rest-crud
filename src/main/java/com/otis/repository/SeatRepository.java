package com.otis.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Seat;
import com.otis.preference.ConstantPreference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class SeatRepository {
	private final Sql2o sql2o;
	private final ElSql bundle;

	public SeatRepository(Sql2o sql2o) {
		this.sql2o = sql2o;
		this.bundle = ElSql.of(ElSqlConfig.MYSQL, SeatRepository.class);
	}

	/**
	 * Execute a supplier within a Sql2o transaction.
	 * Commits on success, rolls back on exception.
	 */
	public <T> T executeInTransaction(Supplier<T> action) {
		try (Connection conn = sql2o.beginTransaction()) {
			T result = action.get();
			conn.commit();
			return result;
		} catch (Exception e) {
			log.error("Transaction failed: ", e);
			throw e;
		}
	}

	/**
	 * Execute a runnable within a Sql2o transaction.
	 * Commits on success, rolls back on exception.
	 */
	public void executeInTransaction(Runnable action) {
		try (Connection conn = sql2o.beginTransaction()) {
			action.run();
			conn.commit();
		} catch (Exception e) {
			log.error("Transaction failed: ", e);
			throw e;
		}
	}

	public PageResponse<Seat> findByEventId(int page, int size, UUID eventId) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.EVENT_ID, eventId.toString());
		params.put(ConstantPreference.SIZE, size);
		params.put(ConstantPreference.OFFSET, page * size);

		String findSql = bundle.getSql("FindByEventId", params);
		String countSql = bundle.getSql("CountByEventId", params);

		log.debug("FindByEventId: {}", findSql);
		log.debug("CountByEventId: {}", countSql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(findSql);
				Query countQuery = conn.createQuery(countSql)) {

			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
				countQuery.addParameter(entry.getKey(), entry.getValue());
			}

			var seats = query.executeAndFetch(Seat.class);
			long totalElements = countQuery.executeAndFetchFirst(Integer.class);

			int totalPages = (int) Math.ceil((double) totalElements / size);
			boolean isFirst = page == 0;
			boolean isLast = page >= totalPages - 1;

			return new PageResponse<>(seats, page, size, totalElements, totalPages, isFirst, isLast);
		}
	}

	/**
	 * Find and lock available seats using SELECT FOR NO KEY UPDATE with ORDER BY.
	 * Lock ordering by ID prevents deadlocks in multi-seat reservations.
	 * Must be called within executeInTransaction().
	 *
	 * @param eventId   the event to find seats for
	 * @param seatCount number of seats needed
	 * @return list of locked seats
	 */
	public List<Seat> findAndLockAvailableSeats(UUID eventId, int seatCount) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.EVENT_ID, eventId.toString());
		params.put(ConstantPreference.LIMIT, seatCount);

		String sql = bundle.getSql("FindAndLockAvailableSeats", params);
		log.debug("FindAndLockAvailableSeats: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			return query.executeAndFetch(Seat.class);
		}
	}

	/**
	 * Lock specific seats by ID in ascending order to prevent deadlocks.
	 * Uses FOR NO KEY UPDATE for better concurrency than FOR UPDATE.
	 * Must be called within a transaction.
	 * 
	 * @param seatIds list of seat IDs to lock (will be sorted internally)
	 * @return list of locked seats
	 */
	
	public List<Seat> lockSeatsByIds(List<UUID> seatIds) {
		if (seatIds == null || seatIds.isEmpty()) {
			return List.of();
		}

		// Sort IDs to ensure consistent lock ordering across all transactions
		List<UUID> sortedIds = seatIds.stream()
				.sorted()
				.toList();

		Map<String, Object> params = new HashMap<>();
		// Build comma-separated list of quoted UUIDs for IN clause
		String idsList = sortedIds.stream()
				.map(UUID::toString)
				.map(id -> "'" + id + "'")
				.reduce((a, b) -> a + ", " + b)
				.orElse("");
		params.put(ConstantPreference.IDS, idsList);

		String sql = bundle.getSql("LockSeatsByIds", params);
		log.debug("LockSeatsByIds: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			query.addParameter(ConstantPreference.IDS, idsList);
			return query.executeAndFetch(Seat.class);
		}
	}

	/**
	 * Reserve seats with optimistic locking using version column.
	 * Updates only if version matches, preventing concurrent modifications.
	 * 
	 * @param seatIds       list of seat IDs to reserve
	 * @param reservationId the reservation ID
	 * @param heldUntil     when the hold expires
	 * @return number of seats successfully reserved
	 */
	
	public int reserveSeatsWithOptimisticLocking(List<UUID> seatIds, UUID reservationId, java.time.Instant heldUntil) {
		if (seatIds == null || seatIds.isEmpty()) {
			return 0;
		}

		String idsList = seatIds.stream()
				.map(UUID::toString)
				.map(id -> "'" + id + "'")
				.reduce((a, b) -> a + ", " + b)
				.orElse("");

		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.IDS, idsList);
		params.put(ConstantPreference.RESERVATION_ID, reservationId.toString());
		params.put(ConstantPreference.HELD_UNTIL, heldUntil.toString());

		String sql = bundle.getSql("ReserveSeatsWithVersion", params);
		log.debug("ReserveSeatsWithVersion: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			query.addParameter(ConstantPreference.IDS, idsList);
			query.addParameter(ConstantPreference.RESERVATION_ID, reservationId.toString());
			query.addParameter(ConstantPreference.HELD_UNTIL, heldUntil.toString());
			return query.executeUpdate().getResult();
		}
	}

	/**
	 * Mark seats as reserved. Must be called within a transaction.
	 * 
	 * @param seatIds       list of seat IDs to reserve
	 * @param reservationId the reservation ID
	 * @return number of seats successfully marked
	 */
	
	public int markSeatsAsReserved(List<UUID> seatIds, UUID reservationId) {
		String sql = bundle.getSql("ReserveSeatsByIds");
		log.debug("ReserveSeatsByIds: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {

			// Build comma-separated list of quoted UUIDs for IN clause
			String idsList = seatIds.stream()
					.map(UUID::toString)
					.map(id -> "'" + id + "'")
					.reduce((a, b) -> a + ", " + b)
					.orElse("");

			query.addParameter(ConstantPreference.IDS, idsList);
			query.addParameter(ConstantPreference.RESERVATION_ID, reservationId.toString());

			return query.executeUpdate().getResult();
		}
	}

	/**
	 * Release seats by reservation ID. Must be called within a transaction.
	 * 
	 * @param reservationId the reservation ID
	 * @return true if any seats were released
	 */
	
	public boolean releaseSeats(UUID reservationId) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.RESERVATION_ID, reservationId.toString());

		String sql = bundle.getSql("ReleaseSeats");
		log.debug("ReleaseSeats: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			int updated = query.executeUpdate().getResult();
			return updated > 0;
		}
	}

	/**
	 * Release specific seats by ID (for partial rollback).
	 * 
	 * @param seatIds list of seat IDs to release
	 * @return number of seats released
	 */
	
	public int releaseSeatsByIds(List<UUID> seatIds) {
		if (seatIds == null || seatIds.isEmpty()) {
			return 0;
		}

		String idsList = seatIds.stream()
				.map(UUID::toString)
				.map(id -> "'" + id + "'")
				.reduce((a, b) -> a + ", " + b)
				.orElse("");

		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.IDS, idsList);

		String sql = bundle.getSql("ReleaseSeatsByIds", params);
		log.debug("ReleaseSeatsByIds: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			query.addParameter(ConstantPreference.IDS, idsList);
			return query.executeUpdate().getResult();
		}
	}

	/**
	 * Confirm reservation - mark seats as booked.
	 * 
	 * @param seatIds       list of seat IDs to confirm
	 * @param reservationId the reservation ID
	 * @return number of seats confirmed
	 */
	
	public int confirmSeats(List<UUID> seatIds, UUID reservationId) {
		String idsList = seatIds.stream()
				.map(UUID::toString)
				.map(id -> "'" + id + "'")
				.reduce((a, b) -> a + ", " + b)
				.orElse("");

		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.IDS, idsList);
		params.put(ConstantPreference.RESERVATION_ID, reservationId.toString());

		String sql = bundle.getSql("ConfirmSeats", params);
		log.debug("ConfirmSeats: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			query.addParameter(ConstantPreference.IDS, idsList);
			query.addParameter(ConstantPreference.RESERVATION_ID, reservationId.toString());
			return query.executeUpdate().getResult();
		}
	}

	/**
	 * Release expired reservations.
	 * This method manages its own transaction using Sql2o.
	 *
	 * @return number of seats released
	 */
	public int releaseExpiredReservations() {
		String sql = bundle.getSql("ReleaseExpiredReservations");
		log.debug("ReleaseExpiredReservations: {}", sql);

		try (Connection conn = sql2o.beginTransaction();
				Query query = conn.createQuery(sql)) {
			int result = query.executeUpdate().getResult();
			conn.commit();
			return result;
		} catch (Exception e) {
			log.error("Error releasing expired reservations: ", e);
			throw e;
		}
	}

	public void insertSeat(UUID id, UUID eventId, String seatNumber) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.ID, id.toString());
		params.put(ConstantPreference.EVENT_ID, eventId.toString());
		params.put(ConstantPreference.SEAT_NUMBER, seatNumber);

		String sql = bundle.getSql("InsertSeat");
		log.debug("InsertSeat: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			query.executeUpdate();
		}
	}

	/**
	 * Get count of available seats for an event.
	 */
	public int countAvailableSeats(UUID eventId) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.EVENT_ID, eventId.toString());

		String sql = bundle.getSql("CountAvailableSeats", params);
		log.debug("CountAvailableSeats: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			query.addParameter(ConstantPreference.EVENT_ID, eventId.toString());
			Integer count = query.executeAndFetchFirst(Integer.class);
			return count != null ? count : 0;
		}
	}
}
