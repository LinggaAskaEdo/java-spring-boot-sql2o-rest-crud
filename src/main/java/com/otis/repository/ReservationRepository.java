package com.otis.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.entity.Reservation;

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
        String sql = bundle.getSql("Create");
        log.info("Create reservation: {}", sql);
        try (Connection conn = sql2o.open()) {
            conn.createQuery(sql)
                    .addParameter("id", reservation.getId().toString())
                    .addParameter("eventId", reservation.getEventId().toString())
                    .addParameter("customerName", reservation.getCustomerName())
                    .addParameter("seatCount", reservation.getSeatCount())
                    .executeUpdate();
        }
    }

    public Reservation findById(UUID id) {
        String sql = bundle.getSql("FindById");
        try (Connection conn = sql2o.open()) {
            return conn.createQuery(sql)
                    .addParameter("id", id.toString())
                    .executeAndFetchFirst(Reservation.class);
        }
    }
}
