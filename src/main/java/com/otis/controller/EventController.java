package com.otis.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.otis.model.Event;
import com.otis.model.PageResponse;
import com.otis.service.EventService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class EventController {
	private final EventService eventService;

	public EventController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping("/events")
	public ResponseEntity<PageResponse<Event>> getAllEvents(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) UUID id,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String venue) {
		return ResponseEntity.ok(eventService.findByFilters(page, size, id, name, venue));
	}

	@GetMapping("/events/{id}")
	public ResponseEntity<Event> getEventById(@PathVariable UUID id) {
		Event event = eventService.findById(id);
		if (event == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(event);
	}

	@GetMapping("/events/{id}/seats/available")
	public ResponseEntity<SeatAvailability> getAvailableSeats(@PathVariable UUID id) {
		int available = eventService.getAvailableSeats(id);
		int total = eventService.getTotalSeats(id);

		return ResponseEntity.ok(new SeatAvailability(total, available));
	}

	public record SeatAvailability(int total, int available) {
	}
}
