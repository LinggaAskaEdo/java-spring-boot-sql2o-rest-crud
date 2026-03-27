package com.otis.repository;

import java.util.List;
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
		StringBuilder sql = new StringBuilder(bundle.getSql("FindByEventId"));
		String countSql = bundle.getSql("CountByEventId");

		int offset = page * size;
		sql.append(" ORDER BY seat_number LIMIT :size OFFSET :offset");

		log.info("FindByEventId: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql.toString());
				Query countQuery = conn.createQuery(countSql)) {
			query.addParameter(ConstantPreference.EVENT_ID, eventId.toString());
			query.addParameter(ConstantPreference.SIZE, size);
			query.addParameter(ConstantPreference.OFFSET, offset);

			countQuery.addParameter(ConstantPreference.EVENT_ID, eventId.toString());

			var seats = query.executeAndFetch(Seat.class);
			long totalElements = countQuery.executeAndFetchFirst(Integer.class);

			int totalPages = (int) Math.ceil((double) totalElements / size);
			boolean isFirst = page == 0;
			boolean isLast = page >= totalPages - 1;

			return new PageResponse<>(seats, page, size, totalElements, totalPages, isFirst, isLast);
		}
	}

	public List<Seat> findAndLockAvailableSeats(UUID eventId, int limit) {
		String sql = bundle.getSql("FindAndLockAvailableSeats");
		log.info("FindAndLockAvailableSeats: {}", sql);

		try (Connection conn = sql2o.open()) {
			return conn.createQuery(sql)
					.addParameter(ConstantPreference.EVENT_ID, eventId.toString())
					.addParameter(ConstantPreference.LIMIT, limit)
					.executeAndFetch(Seat.class);
		}
	}

	public int reserveSeatsInTransaction(List<UUID> seatIds, UUID reservationId) {
		String lockSql = bundle.getSql("LockSeatById");
		String updateSql = bundle.getSql("ReserveSeatById");

		try (Connection conn = sql2o.beginTransaction()) {
			int reserved = 0;

			for (UUID seatId : seatIds) {
				Seat seat = conn.createQuery(lockSql)
						.addParameter(ConstantPreference.ID, seatId.toString())
						.executeAndFetchFirst(Seat.class);

				if (seat != null) {
					int updated = conn.createQuery(updateSql)
							.addParameter(ConstantPreference.ID, seatId.toString())
							.addParameter(ConstantPreference.RESERVATION_ID, reservationId.toString())
							.executeUpdate()
							.getResult();
					reserved += updated;
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
		String sql = bundle.getSql("ReleaseSeats");
		log.info("ReleaseSeats: reservationId={}", reservationId);
		try (Connection conn = sql2o.open()) {
			int updated = conn.createQuery(sql)
					.addParameter(ConstantPreference.RESERVATION_ID, reservationId.toString())
					.executeUpdate()
					.getResult();

			return updated > 0;
		}
	}
}
