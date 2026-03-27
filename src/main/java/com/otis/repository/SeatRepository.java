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
			query.addParameter("size", size);
			query.addParameter("offset", offset);

			countQuery.addParameter(ConstantPreference.EVENT_ID, eventId.toString());

			var seats = query.executeAndFetch(Seat.class);
			long totalElements = countQuery.executeAndFetchFirst(Integer.class);

			int totalPages = (int) Math.ceil((double) totalElements / size);
			boolean isFirst = page == 0;
			boolean isLast = page >= totalPages - 1;

			return PageResponse.<Seat>builder()
					.content(seats)
					.page(page)
					.size(size)
					.totalElements(totalElements)
					.totalPages(totalPages)
					.first(isFirst)
					.last(isLast)
					.build();
		}
	}

	public List<Seat> findAndLockAvailableSeats(UUID eventId, int limit) {
		String sql = bundle.getSql("FindAndLockAvailableSeats");
		log.info("FindAndLockAvailableSeats: {}", sql);
		try (Connection conn = sql2o.open()) {
			return conn.createQuery(sql)
					.addParameter(ConstantPreference.EVENT_ID, eventId.toString())
					.addParameter("limit", limit)
					.executeAndFetch(Seat.class);
		}
	}

	public int reserveSeatsInTransaction(List<UUID> seatIds, UUID reservationId) {
		Connection conn = sql2o.beginTransaction();
		try {
			int reserved = 0;
			for (UUID seatId : seatIds) {
				String lockSql = "SELECT id FROM seats WHERE id = :id AND reserved = FALSE FOR UPDATE";
				Seat seat = conn.createQuery(lockSql)
						.addParameter("id", seatId.toString())
						.executeAndFetchFirst(Seat.class);

				if (seat != null) {
					String updateSql = "UPDATE seats SET reserved = TRUE, reservation_id = :reservationId WHERE id = :id";
					int updated = conn.createQuery(updateSql)
							.addParameter("id", seatId.toString())
							.addParameter("reservationId", reservationId.toString())
							.executeUpdate()
							.getResult();
					reserved += updated;
				}
			}

			conn.commit();
			return reserved;
		} catch (Exception e) {
			log.error("Error reserving seats: ", e);
			conn.rollback();
			return 0;
		} finally {
			conn.close();
		}
	}

	public boolean releaseSeats(UUID reservationId) {
		String sql = bundle.getSql("ReleaseSeats");
		log.info("ReleaseSeats: reservationId={}", reservationId);
		try (Connection conn = sql2o.open()) {
			int updated = conn.createQuery(sql)
					.addParameter("reservationId", reservationId.toString())
					.executeUpdate()
					.getResult();

			return updated > 0;
		}
	}
}
