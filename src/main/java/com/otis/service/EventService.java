package com.otis.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.entity.Event;
import com.otis.model.entity.PageResponse;
import com.otis.repository.EventRepository;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EventService {
	private final EventRepository eventRepository;

	public EventService(EventRepository eventRepository) {
		this.eventRepository = eventRepository;
	}

	@Bulkhead(name = "database")
	@Retry(name = "database")
	public PageResponse<Event> findByFilters(int page, int size, UUID id, String name, String venue) {
		return eventRepository.findByFilters(page, size, id, name, venue);
	}

	@Bulkhead(name = "database")
	@Retry(name = "database")
	public int getAvailableSeats(UUID eventId) {
		return eventRepository.countAvailableSeats(eventId);
	}

	@Bulkhead(name = "database")
	@Retry(name = "database")
	public int getTotalSeats(UUID eventId) {
		return eventRepository.countTotalSeats(eventId);
	}
}
