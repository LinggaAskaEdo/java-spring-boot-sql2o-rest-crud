package com.otis.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.entity.Event;
import com.otis.model.entity.PageResponse;
import com.otis.repository.EventRepository;
import com.otis.util.BulkheadUtils;

import io.github.resilience4j.bulkhead.Bulkhead;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EventService {
	private final EventRepository eventRepository;
	private final Bulkhead bulkhead;

	public EventService(EventRepository eventRepository, Bulkhead databaseBulkhead) {
		this.eventRepository = eventRepository;
		this.bulkhead = databaseBulkhead;
	}

	public PageResponse<Event> findByFilters(int page, int size, UUID id, String name, String venue) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> eventRepository.findByFilters(page, size, id, name, venue), "findByFilters");
	}

	public Event findById(UUID id) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> eventRepository.findById(id), "findById");
	}

	public int getAvailableSeats(UUID eventId) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> eventRepository.countAvailableSeats(eventId), "countAvailableSeats");
	}

	public int getTotalSeats(UUID eventId) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> eventRepository.countTotalSeats(eventId), "countTotalSeats");
	}
}
