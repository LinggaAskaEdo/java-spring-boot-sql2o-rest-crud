package com.otis.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Seat;
import com.otis.repository.ReservationRepository;
import com.otis.repository.ReservationSeatRepository;
import com.otis.repository.SeatRepository;

class SeatServiceTest {
	private SeatRepository seatRepository;
	private ReservationRepository reservationRepository;
	private ReservationSeatRepository reservationSeatRepository;
	private SeatService seatService;

	@BeforeEach
	@SuppressWarnings({ "unused", "unchecked" })
	void setUp() {
		seatRepository = mock(SeatRepository.class);
		reservationRepository = mock(ReservationRepository.class);
		reservationSeatRepository = mock(ReservationSeatRepository.class);
		seatService = new SeatService(seatRepository, reservationRepository, reservationSeatRepository);

		// Mock executeInTransaction to execute the supplier immediately
		when(seatRepository.executeInTransaction(any(java.util.function.Supplier.class)))
				.thenAnswer(invocation -> {
					java.util.function.Supplier<?> supplier = invocation.getArgument(0);
					return supplier.get();
				});
	}

	@Test
	void findByEventId_ReturnsPageResponse() {
		UUID eventId = UUID.randomUUID();
		UUID seatId = UUID.randomUUID();
		Seat seat = new Seat(seatId, eventId, "A1", "available", null, null, 0, false, null);
		PageResponse<Seat> expectedResponse = new PageResponse<>(List.of(seat), 0, 20, 1, 1, true, true);

		when(seatRepository.findByEventId(anyInt(), anyInt(), any())).thenReturn(expectedResponse);

		PageResponse<Seat> result = seatService.findByEventId(0, 20, eventId);

		assertEquals(expectedResponse, result);
		assertEquals(1, result.content().size());
		assertEquals("A1", result.content().get(0).seatNumber());
	}

	@Test
	void findByEventId_ReturnsEmptyList() {
		UUID eventId = UUID.randomUUID();
		PageResponse<Seat> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 20, 0, 0, true, true);

		when(seatRepository.findByEventId(anyInt(), anyInt(), any())).thenReturn(emptyResponse);

		PageResponse<Seat> result = seatService.findByEventId(0, 20, eventId);

		assertEquals(0, result.totalElements());
		assertEquals(0, result.content().size());
	}

	@Test
	void findByEventId_WithPagination_ReturnsCorrectPage() {
		UUID eventId = UUID.randomUUID();
		Seat seat = new Seat(UUID.randomUUID(), eventId, "A1", "available", null, null, 0, false, null);
		PageResponse<Seat> pageResponse = new PageResponse<>(List.of(seat), 1, 5, 10, 2, false, false);

		when(seatRepository.findByEventId(anyInt(), anyInt(), any())).thenReturn(pageResponse);

		PageResponse<Seat> result = seatService.findByEventId(1, 5, eventId);

		assertEquals(1, result.page());
		assertEquals(5, result.size());
		assertEquals(10, result.totalElements());
		assertEquals(2, result.totalPages());
		assertFalse(result.first());
		assertFalse(result.last());
	}

	@Test
	void cancelReservation_WhenSuccessful_ReturnsTrue() {
		UUID reservationId = UUID.randomUUID();

		when(reservationRepository.findById(reservationId)).thenReturn(
				new com.otis.model.entity.Reservation(reservationId, UUID.randomUUID(), "John", 2, "pending", null));
		when(reservationSeatRepository.findByReservationId(reservationId)).thenReturn(Collections.emptyList());
		when(seatRepository.releaseSeatsByIds(any())).thenReturn(2);
		when(reservationSeatRepository.deleteByReservationId(reservationId)).thenReturn(1);
		when(reservationRepository.cancel(reservationId)).thenReturn(true);

		boolean result = seatService.cancelReservation(reservationId);

		assertTrue(result);
	}

	@Test
	void cancelReservation_WhenNotFound_ReturnsFalse() {
		UUID reservationId = UUID.randomUUID();

		when(reservationRepository.findById(reservationId)).thenReturn(null);

		boolean result = seatService.cancelReservation(reservationId);

		assertFalse(result);
	}

	@Test
	void cancelReservation_CallsReleaseSeats() {
		UUID reservationId = UUID.randomUUID();

		when(reservationRepository.findById(reservationId)).thenReturn(
				new com.otis.model.entity.Reservation(reservationId, UUID.randomUUID(), "John", 2, "pending", null));
		when(reservationSeatRepository.findByReservationId(reservationId)).thenReturn(Collections.emptyList());
		when(seatRepository.releaseSeatsByIds(any())).thenReturn(2);
		when(reservationSeatRepository.deleteByReservationId(reservationId)).thenReturn(1);
		when(reservationRepository.cancel(reservationId)).thenReturn(true);

		seatService.cancelReservation(reservationId);

		verify(seatRepository).releaseSeatsByIds(any());
		verify(reservationRepository).cancel(reservationId);
	}
}
