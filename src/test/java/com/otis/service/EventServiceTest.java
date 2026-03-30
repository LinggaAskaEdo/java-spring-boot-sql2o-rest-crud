package com.otis.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.otis.model.entity.Event;
import com.otis.model.entity.PageResponse;
import com.otis.repository.EventRepository;

class EventServiceTest {
	private EventRepository eventRepository;
	private EventService eventService;

	@BeforeEach
	@SuppressWarnings("unused")
	void setUp() {
		eventRepository = mock(EventRepository.class);
		eventService = new EventService(eventRepository);
	}

	@Test
	void findByFilters_ReturnsPageResponse() {
		UUID eventId = UUID.randomUUID();
		Event event = new Event(eventId, "Test Event", "Test Venue");
		PageResponse<Event> expectedResponse = new PageResponse<>(List.of(event), 0, 10, 1, 1, true, true);

		when(eventRepository.findByFilters(anyInt(), anyInt(), any(), any(), any()))
				.thenReturn(expectedResponse);

		PageResponse<Event> result = eventService.findByFilters(0, 10, null, null, null);

		assertEquals(expectedResponse, result);
		assertEquals(1, result.content().size());
		assertEquals("Test Event", result.content().get(0).name());
	}

	@Test
	void findByFilters_WithAllFilters_ReturnsFilteredResult() {
		UUID eventId = UUID.randomUUID();
		Event event = new Event(eventId, "Tech Conference", "Convention Center");
		PageResponse<Event> expectedResponse = new PageResponse<>(List.of(event), 0, 10, 1, 1, true, true);

		when(eventRepository.findByFilters(anyInt(), anyInt(), any(), any(), any()))
				.thenReturn(expectedResponse);

		PageResponse<Event> result = eventService.findByFilters(0, 10, eventId, "Tech", "Convention");

		assertEquals(expectedResponse, result);
		assertEquals("Tech Conference", result.content().get(0).name());
	}

	@Test
	void findByFilters_ReturnsEmptyList() {
		PageResponse<Event> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);

		when(eventRepository.findByFilters(anyInt(), anyInt(), any(), any(), any()))
				.thenReturn(emptyResponse);

		PageResponse<Event> result = eventService.findByFilters(0, 10, null, null, null);

		assertEquals(0, result.totalElements());
		assertEquals(0, result.content().size());
	}

	@Test
	void getAvailableSeats_ReturnsCorrectCount() {
		UUID eventId = UUID.randomUUID();
		int availableSeats = 50;

		when(eventRepository.countAvailableSeats(eventId)).thenReturn(availableSeats);

		int result = eventService.getAvailableSeats(eventId);

		assertEquals(availableSeats, result);
	}

	@Test
	void getTotalSeats_ReturnsCorrectCount() {
		UUID eventId = UUID.randomUUID();
		int totalSeats = 100;

		when(eventRepository.countTotalSeats(eventId)).thenReturn(totalSeats);

		int result = eventService.getTotalSeats(eventId);

		assertEquals(totalSeats, result);
	}

	@Test
	void getAvailableSeats_WhenNoSeats_ReturnsZero() {
		UUID eventId = UUID.randomUUID();

		when(eventRepository.countAvailableSeats(eventId)).thenReturn(0);

		int result = eventService.getAvailableSeats(eventId);

		assertEquals(0, result);
	}
}
