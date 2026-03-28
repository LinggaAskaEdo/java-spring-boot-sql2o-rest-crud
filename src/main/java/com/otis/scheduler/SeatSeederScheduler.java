package com.otis.scheduler;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.otis.repository.EventRepository;
import com.otis.repository.SeatRepository;
import com.otis.util.UuidUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduler.seat-seeder.enabled", havingValue = "true", matchIfMissing = true)
public class SeatSeederScheduler {
	private final EventRepository eventRepository;
	private final SeatRepository seatRepository;

	@EventListener(ApplicationReadyEvent.class)
	public void seedData() {
		try {
			if (!isDataExists()) {
				createEvent();
				createSeats();
			}
		} catch (Exception e) {
			log.error("Error seeding data: {}", e.getMessage(), e);
		}
	}

	private boolean isDataExists() {
		int count = eventRepository.countEvents();
		return count > 0;
	}

	private void createEvent() {
		UUID eventId = UuidUtils.randomUuidV7();
		eventRepository.insertEvent(eventId, "Tech Conference 2026", "Convention Center Hall A");
		log.info("Created event: Tech Conference 2026 with ID: {}", eventId);
	}

	private void createSeats() {
		UUID eventId = eventRepository.findFirstEventId();
		if (eventId == null) {
			log.error("No event found to create seats for");
			return;
		}

		String[] rows = { "A", "B", "C", "D", "E" };
		for (String row : rows) {
			for (int num = 1; num <= 10; num++) {
				UUID seatId = UuidUtils.randomUuidV7();
				String seatNumber = row + num;
				seatRepository.insertSeat(seatId, eventId, seatNumber);
			}
		}

		log.info("Created 50 seats for event: {}", eventId);
	}
}
