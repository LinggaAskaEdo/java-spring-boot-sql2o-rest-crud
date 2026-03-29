package com.otis.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.otis.model.entity.Event;
import com.otis.model.entity.PageResponse;
import com.otis.repository.EventRepository;
import com.otis.util.BulkheadUtils;

import io.github.resilience4j.bulkhead.Bulkhead;

class EventServiceTest {
	private EventRepository eventRepository;
	private Bulkhead bulkhead;
	private EventService eventService;

	@BeforeEach
	@SuppressWarnings("unused")
	void setUp() {
		eventRepository = mock(EventRepository.class);
		bulkhead = mock(Bulkhead.class);
		eventService = new EventService(eventRepository, bulkhead);
	}

	@Test
	@SuppressWarnings("unchecked")
	void findByFilters_ReturnsPageResponse() {
		UUID eventId = UUID.randomUUID();
		Event event = new Event(eventId, "Test Event", "Test Venue");
		PageResponse<Event> expectedResponse = new PageResponse<>(List.of(event), 0, 10, 1, 1, true, true);

		when(eventRepository.findByFilters(anyInt(), anyInt(), any(), any(), any()))
				.thenReturn(expectedResponse);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils
					.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			PageResponse<Event> result = eventService.findByFilters(0, 10, null, null, null);

			assertEquals(expectedResponse, result);
			assertEquals(1, result.content().size());
			assertEquals("Test Event", result.content().get(0).name());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void findByFilters_WithAllFilters_ReturnsFilteredResult() {
		UUID eventId = UUID.randomUUID();
		Event event = new Event(eventId, "Tech Conference", "Convention Center");
		PageResponse<Event> expectedResponse = new PageResponse<>(List.of(event), 0, 10, 1, 1, true, true);

		when(eventRepository.findByFilters(anyInt(), anyInt(), any(), any(), any()))
				.thenReturn(expectedResponse);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils
					.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			PageResponse<Event> result = eventService.findByFilters(0, 10, eventId, "Tech", "Convention");

			assertEquals(expectedResponse, result);
			assertEquals("Tech Conference", result.content().get(0).name());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void findByFilters_ReturnsEmptyList() {
		PageResponse<Event> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);

		when(eventRepository.findByFilters(anyInt(), anyInt(), any(), any(), any()))
				.thenReturn(emptyResponse);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils
					.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			PageResponse<Event> result = eventService.findByFilters(0, 10, null, null, null);

			assertEquals(0, result.totalElements());
			assertEquals(0, result.content().size());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void findById_WhenEventExists_ReturnsEvent() {
		UUID eventId = UUID.randomUUID();
		Event event = new Event(eventId, "Test Event", "Test Venue");

		when(eventRepository.findById(eventId)).thenReturn(event);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils
					.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			Event result = eventService.findById(eventId);

			assertNotNull(result);
			assertEquals(eventId, result.id());
			assertEquals("Test Event", result.name());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void findById_WhenEventNotExists_ReturnsNull() {
		UUID eventId = UUID.randomUUID();

		when(eventRepository.findById(eventId)).thenReturn(null);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils
					.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			Event result = eventService.findById(eventId);

			assertNull(result);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void getAvailableSeats_ReturnsCorrectCount() {
		UUID eventId = UUID.randomUUID();
		int availableSeats = 50;

		when(eventRepository.countAvailableSeats(eventId)).thenReturn(availableSeats);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils
					.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			int result = eventService.getAvailableSeats(eventId);

			assertEquals(availableSeats, result);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void getTotalSeats_ReturnsCorrectCount() {
		UUID eventId = UUID.randomUUID();
		int totalSeats = 100;

		when(eventRepository.countTotalSeats(eventId)).thenReturn(totalSeats);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils
					.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			int result = eventService.getTotalSeats(eventId);

			assertEquals(totalSeats, result);
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void getAvailableSeats_WhenNoSeats_ReturnsZero() {
		UUID eventId = UUID.randomUUID();

		when(eventRepository.countAvailableSeats(eventId)).thenReturn(0);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils
					.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			int result = eventService.getAvailableSeats(eventId);

			assertEquals(0, result);
		}
	}
}
