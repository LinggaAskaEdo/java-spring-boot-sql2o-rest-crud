package com.otis.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.entity.Reservation;
import com.otis.preference.ConstantPreference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class ReservationRepository {
	private final Sql2o sql2o;
	private final ElSql bundle;

	public ReservationRepository(Sql2o sql2o) {
		this.sql2o = sql2o;
		this.bundle = ElSql.of(ElSqlConfig.MYSQL, ReservationRepository.class);
	}

	/**
	 * Create a new reservation with pending status.
	 * Must be called within executeInTransaction().
	 */
	public Reservation create(Reservation reservation) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.ID, reservation.id().toString());
		params.put(ConstantPreference.EVENT_ID, reservation.eventId().toString());
		params.put(ConstantPreference.CUSTOMER_NAME, reservation.customerName());
		params.put(ConstantPreference.SEAT_COUNT, reservation.seatCount());
		params.put(ConstantPreference.STATUS, "pending");
		params.put(ConstantPreference.EXPIRES_AT, java.time.Instant.now().plusSeconds(600).toString());

		String sql = bundle.getSql("Create", params);
		log.debug("Create reservation: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			query.executeUpdate();
			return reservation;
		}
	}

	public Reservation findById(UUID id) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.ID, id.toString());

		String sql = bundle.getSql("FindById", params);
		log.debug("Find by reservation id: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			return query.executeAndFetchFirst(Reservation.class);
		}
	}

	/**
	 * Update reservation status.
	 * Public method for external callers.
	 */
	public boolean updateStatus(UUID id, String status) {
		return updateStatusInternal(id, status);
	}

	/**
	 * Confirm reservation - change status to confirmed.
	 */
	public boolean confirm(UUID id) {
		return updateStatusInternal(id, "confirmed");
	}

	/**
	 * Cancel reservation - change status to cancelled.
	 */
	public boolean cancel(UUID id) {
		return updateStatusInternal(id, "cancelled");
	}

	/**
	 * Expire reservation - change status to expired.
	 */
	public boolean expire(UUID id) {
		return updateStatusInternal(id, "expired");
	}

	/**
	 * Internal status update implementation.
	 */
	private boolean updateStatusInternal(UUID id, String status) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.ID, id.toString());
		params.put(ConstantPreference.STATUS, status);

		String sql = bundle.getSql("UpdateStatus", params);
		log.debug("Update reservation status: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			int updated = query.executeUpdate().getResult();
			return updated > 0;
		}
	}

	public boolean deleteById(UUID id) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.ID, id.toString());

		String sql = bundle.getSql("DeleteById", params);
		log.debug("Delete reservation: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			int deleted = query.executeUpdate().getResult();
			return deleted > 0;
		}
	}

	/**
	 * Get count of pending reservations for a customer.
	 */
	public int countPendingByCustomer(UUID eventId, String customerName) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.EVENT_ID, eventId.toString());
		params.put(ConstantPreference.CUSTOMER_NAME, customerName);

		String sql = bundle.getSql("CountPendingByCustomer", params);
		log.debug("CountPendingByCustomer: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			query.addParameter(ConstantPreference.EVENT_ID, eventId.toString());
			query.addParameter(ConstantPreference.CUSTOMER_NAME, customerName);
			Integer count = query.executeAndFetchFirst(Integer.class);
			return count != null ? count : 0;
		}
	}
}
