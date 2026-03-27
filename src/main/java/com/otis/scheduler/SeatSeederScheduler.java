package com.otis.scheduler;

import java.util.UUID;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

import com.otis.preference.ConstantPreference;
import com.otis.util.UuidUtils;

@Component
public class SeatSeederScheduler {

    private final Sql2o sql2o;

    public SeatSeederScheduler(Sql2o sql2o) {
        this.sql2o = sql2o;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedData() {
        try {
            if (!isDataExists()) {
                createEvent();
                createSeats();
            }
        } catch (Exception e) {
            System.err.println("Error seeding data: " + e.getMessage());
        }
    }

    private boolean isDataExists() {
        String sql = "SELECT COUNT(*) FROM events";
        try (Connection conn = sql2o.open()) {
            Integer count = conn.createQuery(sql).executeAndFetchFirst(Integer.class);
            return count != null && count > 0;
        }
    }

    private void createEvent() {
        UUID eventId = UuidUtils.randomUuidV7();
        String sql = "INSERT INTO events (id, name, venue) VALUES (:id, :name, :venue)";
        try (Connection conn = sql2o.open()) {
            conn.createQuery(sql)
                    .addParameter(ConstantPreference.ID, eventId.toString())
                    .addParameter(ConstantPreference.NAME, "Tech Conference 2026")
                    .addParameter(ConstantPreference.VENUE, "Convention Center Hall A")
                    .executeUpdate();
            System.out.println("Created event: Tech Conference 2026 with ID: " + eventId);
        }
    }

    private void createSeats() {
        String getEventSql = "SELECT id FROM events LIMIT 1";
        UUID eventId;
        try (Connection conn = sql2o.open()) {
            String idStr = conn.createQuery(getEventSql).executeAndFetchFirst(String.class);
            if (idStr == null) {
                System.err.println("No event found to create seats for");
                return;
            }
            eventId = UuidUtils.parseUUID(idStr);
        }

        String[] rows = { "A", "B", "C", "D", "E" };
        String sql = "INSERT INTO seats (id, event_id, seat_number, reserved) VALUES (:id, :eventId, :seatNumber, 0)";

        try (Connection conn = sql2o.open()) {
            for (String row : rows) {
                for (int num = 1; num <= 10; num++) {
                    UUID seatId = UuidUtils.randomUuidV7();
                    String seatNumber = row + num;
                    conn.createQuery(sql)
                            .addParameter(ConstantPreference.ID, seatId.toString())
                            .addParameter(ConstantPreference.EVENT_ID, eventId.toString())
                            .addParameter(ConstantPreference.SEAT_NUMBER, seatNumber)
                            .executeUpdate();
                }
            }
            System.out.println("Created 50 seats for event: " + eventId);
        }
    }
}
