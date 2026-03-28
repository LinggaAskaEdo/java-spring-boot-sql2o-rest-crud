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

	public void create(Reservation reservation) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.ID, reservation.id().toString());
		params.put(ConstantPreference.EVENT_ID, reservation.eventId().toString());
		params.put(ConstantPreference.CUSTOMER_NAME, reservation.customerName());
		params.put(ConstantPreference.SEAT_COUNT, reservation.seatCount());

		String sql = bundle.getSql("Create", params);
		log.info("Create reservation: {}", sql);

		try (Connection conn = sql2o.open(); Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			query.executeUpdate();
		}
	}

	public Reservation findById(UUID id) {
		Map<String, Object> params = new HashMap<>();
		params.put(ConstantPreference.ID, id.toString());

		String sql = bundle.getSql("FindById", params);
		log.info("Find by reservation id: {}", sql);

		try (Connection conn = sql2o.open(); Query query = conn.createQuery(sql)) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			return query.executeAndFetchFirst(Reservation.class);
		}
	}
}