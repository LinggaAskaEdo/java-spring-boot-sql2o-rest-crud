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
import com.otis.model.entity.Event;
import com.otis.model.entity.PageResponse;
import com.otis.preference.ConstantPreference;
import com.otis.util.UuidUtils;

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
		Map<String, Object> filterParams = new HashMap<>();
		if (id != null) {
			filterParams.put(ConstantPreference.ID, id.toString());
		}

		if (name != null && !name.isBlank()) {
			filterParams.put(ConstantPreference.NAME, "%" + name + "%");
		}

		if (venue != null && !venue.isBlank()) {
			filterParams.put(ConstantPreference.VENUE, "%" + venue + "%");
		}

		int offset = page * size;
		Map<String, Object> pagingParams = new HashMap<>(filterParams);
		pagingParams.put(ConstantPreference.SIZE, size);
		pagingParams.put(ConstantPreference.OFFSET, offset);

		String findSql = bundle.getSql("FindByFilters", pagingParams);
		String countSql = bundle.getSql("CountByFilters", filterParams);

		log.debug("FindByFilters: {}", findSql);
		log.debug("CountByFilters: {}", countSql);

		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(findSql);
				Query countQuery = conn.createQuery(countSql)) {

			for (Map.Entry<String, Object> entry : pagingParams.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			for (Map.Entry<String, Object> entry : filterParams.entrySet()) {
				countQuery.addParameter(entry.getKey(), entry.getValue());
			}

			var events = query.executeAndFetch(Event.class);
			long totalElements = countQuery.executeAndFetchFirst(Integer.class);

			int totalPages = (int) Math.ceil((double) totalElements / size);
			boolean isFirst = page == 0;
			boolean isLast = page >= totalPages - 1;

			return new PageResponse<>(events, page, size, totalElements, totalPages, isFirst, isLast);
		}
	}

	public int countAvailableSeats(UUID eventId) {
		String sql = bundle.getSql("CountAvailableSeats");
		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			Integer count = query.addParameter(ConstantPreference.EVENT_ID, eventId.toString())
					.executeAndFetchFirst(Integer.class);
			return count != null ? count : 0;
		}
	}

	public int countTotalSeats(UUID eventId) {
		String sql = bundle.getSql("CountTotalSeats");
		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			Integer count = query.addParameter(ConstantPreference.EVENT_ID, eventId.toString())
					.executeAndFetchFirst(Integer.class);
			return count != null ? count : 0;
		}
	}

	public int countEvents() {
		String sql = bundle.getSql("CountEvents");
		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			Integer count = query.executeAndFetchFirst(Integer.class);
			return count != null ? count : 0;
		}
	}

	public void insertEvent(UUID id, String name, String venue) {
		String sql = bundle.getSql("InsertEvent");
		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			query.addParameter(ConstantPreference.ID, id.toString())
					.addParameter(ConstantPreference.NAME, name)
					.addParameter(ConstantPreference.VENUE, venue)
					.executeUpdate();
		}
	}

	public UUID findFirstEventId() {
		String sql = bundle.getSql("FindFirstEvent");
		try (Connection conn = sql2o.open();
				Query query = conn.createQuery(sql)) {
			String idStr = query.executeAndFetchFirst(String.class);
			return idStr != null ? UuidUtils.parseUUID(idStr) : null;
		}
	}
}
