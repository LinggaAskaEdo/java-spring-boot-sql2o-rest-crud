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

	public PageResponse<Seat> findByEventId(int page, int size, UUID eventId) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.EVENT_ID, eventId.toString());
		params.put(ConstantPreference.SIZE, size);
		params.put(ConstantPreference.OFFSET, page * size);

		String findSql = bundle.getSql("FindByEventId", params);
		String countSql = bundle.getSql("CountByEventId", params);

		log.info("FindByEventId: {}", findSql);
		log.info("CountByEventId: {}", countSql);

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

	public List<Seat> findAndLockAvailableSeats(UUID eventId, int limit) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.EVENT_ID, eventId.toString());
		params.put(ConstantPreference.LIMIT, limit);

		String sql = bundle.getSql("FindAndLockAvailableSeats", params);
		log.info("FindAndLockAvailableSeats: {}", sql);

		try (Connection conn = sql2o.open(); Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			return query.executeAndFetch(Seat.class);
		}
	}

	public int reserveSeatsInTransaction(List<UUID> seatIds, UUID reservationId) {
		String lockSql = bundle.getSql("LockSeatById");
		String updateSql = bundle.getSql("ReserveSeatById");

		try (Connection conn = sql2o.beginTransaction()) {
			int reserved = 0;

			for (UUID seatId : seatIds) {
				Map<String, Object> lockParams = new HashMap<>();
				lockParams.put(ConstantPreference.ID, seatId.toString());

				Map<String, Object> updateParams = new HashMap<>();
				updateParams.put(ConstantPreference.ID, seatId.toString());
				updateParams.put(ConstantPreference.RESERVATION_ID, reservationId.toString());

				// Lock the seat
				try (Query lockQuery = conn.createQuery(lockSql)) {
					for (Map.Entry<String, Object> entry : lockParams.entrySet()) {
						lockQuery.addParameter(entry.getKey(), entry.getValue());
					}

					Seat seat = lockQuery.executeAndFetchFirst(Seat.class);

					if (seat != null) {
						// Update the seat
						try (Query updateQuery = conn.createQuery(updateSql)) {
							for (Map.Entry<String, Object> entry : updateParams.entrySet()) {
								updateQuery.addParameter(entry.getKey(), entry.getValue());
							}

							int updated = updateQuery.executeUpdate().getResult();
							reserved += updated;
						}
					}
				}
			}

			conn.commit();

			return reserved;
		} catch (Exception e) {
			log.error("Error reserving seats: ", e);

			return 0;
		}
	}

	public boolean releaseSeats(UUID reservationId) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.RESERVATION_ID, reservationId.toString());

		String sql = bundle.getSql("ReleaseSeats");
		log.info("ReleaseSeats: {}", sql);

		try (Connection conn = sql2o.open(); Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			int updated = query.executeUpdate().getResult();

			return updated > 0;
		}
	}

	public void insertSeat(UUID id, UUID eventId, String seatNumber) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.ID, id.toString());
		params.put(ConstantPreference.EVENT_ID, eventId.toString());
		params.put(ConstantPreference.SEAT_NUMBER, seatNumber);

		String sql = bundle.getSql("InsertSeat");
		log.info("InsertSeat: {}", sql);

		try (Connection conn = sql2o.open(); Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			query.executeUpdate();
		}
	}
}