package com.otis.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.entity.Event;
import com.otis.model.entity.PageResponse;
import com.otis.preference.ConstantPreference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class EventRepository {
	private final Sql2o sql2o;
	private final ElSql bundle;

	public EventRepository(Sql2o sql2o) {
		this.sql2o = sql2o;
		this.bundle = ElSql.of(ElSqlConfig.MYSQL, EventRepository.class);
	}

	public PageResponse<Event> findByFilters(int page, int size, UUID id, String name, String venue) {
		StringBuilder sql = new StringBuilder(bundle.getSql("FindByFilters"));
		StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM events");
		String separator = ConstantPreference.WHERE;

		if (id != null) {
			sql.append(separator).append("id = :id");
			countSql.append(separator).append("id = :id");
			separator = ConstantPreference.AND;
		}

		if (name != null && !name.isBlank()) {
			sql.append(separator).append("name LIKE :name");
			countSql.append(separator).append("name LIKE :name");
			separator = ConstantPreference.AND;
		}

		if (venue != null && !venue.isBlank()) {
			sql.append(separator).append("venue LIKE :venue");
			countSql.append(separator).append("venue LIKE :venue");
		}

		int offset = page * size;
		sql.append(" LIMIT :size OFFSET :offset");

		log.info("FindByFilters: {}", sql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql.toString());
				Query countQuery = conn.createQuery(countSql.toString())) {

			if (id != null) {
				query.addParameter("id", id.toString());
				countQuery.addParameter("id", id.toString());
			}

			if (name != null && !name.isBlank()) {
				query.addParameter("name", "%" + name + "%");
				countQuery.addParameter("name", "%" + name + "%");
			}

			if (venue != null && !venue.isBlank()) {
				query.addParameter("venue", "%" + venue + "%");
				countQuery.addParameter("venue", "%" + venue + "%");
			}

			query.addParameter("size", size);
			query.addParameter("offset", offset);

			var events = query.executeAndFetch(Event.class);
			long totalElements = countQuery.executeAndFetchFirst(Integer.class);

			int totalPages = (int) Math.ceil((double) totalElements / size);
			boolean isFirst = page == 0;
			boolean isLast = page >= totalPages - 1;

			return PageResponse.<Event>builder()
					.content(events)
					.page(page)
					.size(size)
					.totalElements(totalElements)
					.totalPages(totalPages)
					.first(isFirst)
					.last(isLast)
					.build();
		}
	}

	public Event findById(UUID id) {
		String sql = bundle.getSql("FindById");
		try (Connection conn = sql2o.open(); Query query = conn.createQuery(sql)) {
			return query.addParameter("id", id.toString())
					.executeAndFetchFirst(Event.class);
		}
	}

	public int countAvailableSeats(UUID eventId) {
		String sql = bundle.getSql("CountAvailableSeats");
		try (Connection conn = sql2o.open(); Query query = conn.createQuery(sql)) {
			Integer count = query.addParameter("eventId", eventId.toString())
					.executeAndFetchFirst(Integer.class);
			return count != null ? count : 0;
		}
	}

	public int countTotalSeats(UUID eventId) {
		String sql = bundle.getSql("CountTotalSeats");
		try (Connection conn = sql2o.open(); Query query = conn.createQuery(sql)) {
			Integer count = query.addParameter("eventId", eventId.toString())
					.executeAndFetchFirst(Integer.class);
			return count != null ? count : 0;
		}
	}
}
