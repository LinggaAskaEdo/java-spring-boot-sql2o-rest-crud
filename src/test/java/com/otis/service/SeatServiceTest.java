package com.otis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Seat;
import com.otis.repository.ReservationRepository;
import com.otis.repository.SeatRepository;
import com.otis.util.BulkheadUtils;

import io.github.resilience4j.bulkhead.Bulkhead;

class SeatServiceTest {

	private SeatRepository seatRepository;
	private ReservationRepository reservationRepository;
	private Bulkhead bulkhead;
	private SeatService seatService;

	@BeforeEach
	void setUp() {
		seatRepository = mock(SeatRepository.class);
		reservationRepository = mock(ReservationRepository.class);
		bulkhead = mock(Bulkhead.class);
		seatService = new SeatService(seatRepository, reservationRepository, bulkhead);
	}

	@Test
	void findByEventId_ReturnsPageResponse() {
		UUID eventId = UUID.randomUUID();
		UUID seatId = UUID.randomUUID();
		Seat seat = new Seat(seatId, eventId, "A1", false, null);
		PageResponse<Seat> expectedResponse = new PageResponse<>(List.of(seat), 0, 20, 1, 1, true, true);

		when(seatRepository.findByEventId(anyInt(), anyInt(), any())).thenReturn(expectedResponse);

		try (MockedStatic<BulkheadUtils> mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			PageResponse<Seat> result = seatService.findByEventId(0, 20, eventId);

			assertEquals(expectedResponse, result);
			assertEquals(1, result.content().size());
			assertEquals("A1", result.content().get(0).seatNumber());
		}
	}

	@Test
	void findByEventId_ReturnsEmptyList() {
		UUID eventId = UUID.randomUUID();
		PageResponse<Seat> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 20, 0, 0, true, true);

		when(seatRepository.findByEventId(anyInt(), anyInt(), any())).thenReturn(emptyResponse);

		try (MockedStatic<BulkheadUtils> mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			PageResponse<Seat> result = seatService.findByEventId(0, 20, eventId);

			assertEquals(0, result.totalElements());
			assertEquals(0, result.content().size());
		}
	}

	@Test
	void findByEventId_WithPagination_ReturnsCorrectPage() {
		UUID eventId = UUID.randomUUID();
		Seat seat = new Seat(UUID.randomUUID(), eventId, "A1", false, null);
		PageResponse<Seat> pageResponse = new PageResponse<>(List.of(seat), 1, 5, 10, 2, false, false);

		when(seatRepository.findByEventId(anyInt(), anyInt(), any())).thenReturn(pageResponse);

		try (MockedStatic<BulkheadUtils> mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			PageResponse<Seat> result = seatService.findByEventId(1, 5, eventId);

			assertEquals(1, result.page());
			assertEquals(5, result.size());
			assertEquals(10, result.totalElements());
			assertEquals(2, result.totalPages());
			assertFalse(result.first());
			assertFalse(result.last());
		}
	}

	@Test
	void cancelReservation_WhenSuccessful_ReturnsTrue() {
		UUID reservationId = UUID.randomUUID();

		when(seatRepository.releaseSeats(reservationId)).thenReturn(true);

		try (MockedStatic<BulkheadUtils> mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			boolean result = seatService.cancelReservation(reservationId);

			assertTrue(result);
		}
	}

	@Test
	void cancelReservation_WhenNotFound_ReturnsFalse() {
		UUID reservationId = UUID.randomUUID();

		when(seatRepository.releaseSeats(reservationId)).thenReturn(false);

		try (MockedStatic<BulkheadUtils> mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			boolean result = seatService.cancelReservation(reservationId);

			assertFalse(result);
		}
	}

	@Test
	void cancelReservation_CallsReleaseSeats() {
		UUID reservationId = UUID.randomUUID();

		when(seatRepository.releaseSeats(reservationId)).thenReturn(true);

		try (MockedStatic<BulkheadUtils> mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			seatService.cancelReservation(reservationId);

			verify(seatRepository).releaseSeats(reservationId);
		}
	}
}
