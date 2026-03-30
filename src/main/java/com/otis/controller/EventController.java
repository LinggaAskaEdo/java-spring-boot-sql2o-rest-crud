package com.otis.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.otis.model.dto.SeatAvailability;
import com.otis.model.entity.Event;
import com.otis.model.entity.PageResponse;
import com.otis.service.EventService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@CrossOrigin(origins = "${api.cors.allowed-origins:*}")
@RestController
@RequestMapping("/api/v1")
public class EventController {
	private final EventService eventService;
	private final int maxPageSize;
	private final int defaultPageSize;

	public EventController(EventService eventService,
			@Value("${api.pagination.max-page-size:100}") int maxPageSize,
			@Value("${api.pagination.default-page-size:10}") int defaultPageSize) {
		this.eventService = eventService;
		this.maxPageSize = maxPageSize;
		this.defaultPageSize = defaultPageSize;
	}

	@GetMapping("/events")
	public ResponseEntity<PageResponse<Event>> getAllEvents(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "") String size,
			@RequestParam(required = false) UUID id,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) String venue) {

		// Apply pagination limits
		int effectiveSize = parseSize(size, defaultPageSize);

		return ResponseEntity.ok(eventService.findByFilters(page, effectiveSize, id, name, venue));
	}

	private int parseSize(String size, int defaultSize) {
		if (size == null || size.isBlank()) {
			return defaultSize;
		}

		try {
			int parsed = Integer.parseInt(size);
			return (parsed <= 0) ? defaultSize : Math.min(parsed, maxPageSize);
		} catch (NumberFormatException e) {
			return defaultSize;
		}
	}

	@GetMapping("/events/{id}/seats/available")
	public ResponseEntity<SeatAvailability> getAvailableSeats(@PathVariable UUID id) {
		int available = eventService.getAvailableSeats(id);
		int total = eventService.getTotalSeats(id);

		return ResponseEntity.ok(new SeatAvailability(total, available));
	}
}
