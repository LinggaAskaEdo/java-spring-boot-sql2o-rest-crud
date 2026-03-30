package com.otis.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.entity.ReservationSeat;
import com.otis.preference.ConstantPreference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class ReservationSeatRepository {
	private final Sql2o sql2o;
	private final ElSql bundle;

	public ReservationSeatRepository(Sql2o sql2o) {
		this.sql2o = sql2o;
		this.bundle = ElSql.of(ElSqlConfig.MYSQL, ReservationSeatRepository.class);
	}

	/**
	 * Create reservation-seat relationships.
	 * Must be called within executeInTransaction().
	 *
	 * @param reservationId the reservation ID
	 * @param seatIds       list of seat IDs to link
	 * @return number of relationships created
	 */
	public int createReservationSeats(UUID reservationId, List<UUID> seatIds) {
		if (seatIds == null || seatIds.isEmpty()) {
			return 0;
		}

		int created = 0;
		String sql = bundle.getSql("Create");

		for (UUID seatId : seatIds) {
			try (Connection conn = sql2o.open();
					Query query = conn.createQuery(sql)) {
				query.addParameter(ConstantPreference.ID, UUID.randomUUID().toString())
						.addParameter(ConstantPreference.RESERVATION_ID, reservationId.toString())
						.addParameter(ConstantPreference.SEAT_ID, seatId.toString());
				query.executeUpdate();
				created++;
			}
		}

		return created;
	}

	/**
	 * Get all seats for a reservation.
	 */
	public List<ReservationSeat> findByReservationId(UUID reservationId) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.RESERVATION_ID, reservationId.toString());

		String sql = bundle.getSql("FindByReservationId", params);
		log.debug("FindByReservationId: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			query.addParameter(ConstantPreference.RESERVATION_ID, reservationId.toString());
			return query.executeAndFetch(ReservationSeat.class);
		}
	}

	/**
	 * Delete reservation-seat relationships by reservation ID.
	 */
	public int deleteByReservationId(UUID reservationId) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.RESERVATION_ID, reservationId.toString());

		String sql = bundle.getSql("DeleteByReservationId", params);
		log.debug("DeleteByReservationId: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			query.addParameter(ConstantPreference.RESERVATION_ID, reservationId.toString());
			return query.executeUpdate().getResult();
		}
	}
}
