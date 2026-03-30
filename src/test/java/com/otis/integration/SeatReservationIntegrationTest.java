package com.otis.integration;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.otis.exception.BadRequestException;
import com.otis.exception.ResourceNotFoundException;
import com.otis.model.entity.Reservation;
import com.otis.model.entity.Seat;
import com.otis.repository.ReservationRepository;
import com.otis.repository.ReservationSeatRepository;
import com.otis.repository.SeatRepository;
import com.otis.service.SeatService;
import com.otis.util.UuidUtils;

/**
 * Integration tests for SeatService reservation flow.
 * Tests the critical path of seat reservation with proper transaction handling.
 *
 * Based on race.txt best practices:
 * - Deadlock prevention with ORDER BY lock ordering
 * - FOR NO KEY UPDATE for better concurrency
 * - Partial rollback on failure
 * - Two-phase reservation (reserve → confirm)
 */
@ExtendWith(MockitoExtension.class)
class SeatReservationIntegrationTest {
	@Mock
	private SeatRepository seatRepository;

	@Mock
	private ReservationRepository reservationRepository;

	@Mock
	private ReservationSeatRepository reservationSeatRepository;

	private SeatService seatService;

	@BeforeEach
	@SuppressWarnings({ "unused", "unchecked" })
	void setUp() {
		seatService = new SeatService(seatRepository, reservationRepository, reservationSeatRepository);

		// Mock executeInTransaction to execute the supplier immediately (lenient to
		// avoid unnecessary stubbing errors)
		org.mockito.Mockito.lenient().when(seatRepository.executeInTransaction(any(java.util.function.Supplier.class)))
				.thenAnswer(invocation -> {
					java.util.function.Supplier<?> supplier = invocation.getArgument(0);
					return supplier.get();
				});
	}

	@Test
	void reserveSeats_WhenSufficientSeatsAvailable_ShouldSucceed() {
		// Arrange
		UUID eventId = UUID.randomUUID();
		UUID reservationId = UUID.randomUUID();
		String customerName = "John Doe";
		int seatCount = 3;

		Seat seat1 = createTestSeat(eventId, "A1");
		Seat seat2 = createTestSeat(eventId, "A2");
		Seat seat3 = createTestSeat(eventId, "A3");

		try (MockedStatic<UuidUtils> mockedUuidUtils = mockStatic(UuidUtils.class)) {

			mockedUuidUtils.when(UuidUtils::randomUuidV7).thenReturn(reservationId);

			when(seatRepository.findAndLockAvailableSeats(eventId, seatCount))
					.thenReturn(List.of(seat1, seat2, seat3));
			when(seatRepository.markSeatsAsReserved(any(), any())).thenReturn(seatCount);
			when(reservationSeatRepository.createReservationSeats(any(), any())).thenReturn(seatCount);

			// Act
			Reservation result = seatService.reserveSeats(eventId, customerName, seatCount);

			// Assert
			assertNotNull(result);
			assertEquals(reservationId, result.id());
			assertEquals(eventId, result.eventId());
			assertEquals(customerName, result.customerName());
			assertEquals(seatCount, result.seatCount());
			verify(reservationRepository).create(result);
			verify(reservationSeatRepository).createReservationSeats(any(), any());
		}
	}

	@Test
	void reserveSeats_WhenNoSeatsAvailable_ShouldThrowException() {
		// Arrange
		UUID eventId = UUID.randomUUID();
		String customerName = "John Doe";
		int seatCount = 3;

		when(seatRepository.findAndLockAvailableSeats(eventId, seatCount))
				.thenReturn(Collections.emptyList());

		// Act & Assert
		ResourceNotFoundException exception = assertThrows(
				ResourceNotFoundException.class,
				() -> seatService.reserveSeats(eventId, customerName, seatCount));

		assertEquals("No seats available for the requested event", exception.getMessage());
	}

	@Test
	void reserveSeats_WhenNotEnoughSeatsAvailable_ShouldThrowException() {
		// Arrange
		UUID eventId = UUID.randomUUID();
		String customerName = "John Doe";
		int requestedSeats = 5;

		Seat seat1 = createTestSeat(eventId, "A1");
		Seat seat2 = createTestSeat(eventId, "A2");
		Seat seat3 = createTestSeat(eventId, "A3");

		when(seatRepository.findAndLockAvailableSeats(eventId, requestedSeats))
				.thenReturn(List.of(seat1, seat2, seat3));

		// Act & Assert
		BadRequestException exception = assertThrows(
				BadRequestException.class,
				() -> seatService.reserveSeats(eventId, customerName, requestedSeats));

		assertTrue(exception.getMessage().contains("seats available"));
	}

	@Test
	void reserveSeats_WhenMarkSeatsFails_ShouldRollback() {
		// Arrange
		UUID eventId = UUID.randomUUID();
		String customerName = "John Doe";
		int seatCount = 3;

		Seat seat1 = createTestSeat(eventId, "A1");
		Seat seat2 = createTestSeat(eventId, "A2");
		Seat seat3 = createTestSeat(eventId, "A3");

		try (MockedStatic<UuidUtils> mockedUuidUtils = mockStatic(UuidUtils.class)) {

			mockedUuidUtils.when(UuidUtils::randomUuidV7).thenReturn(UUID.randomUUID());

			when(seatRepository.findAndLockAvailableSeats(eventId, seatCount))
					.thenReturn(List.of(seat1, seat2, seat3));
			// Simulate partial failure - only 2 seats marked instead of 3
			when(seatRepository.markSeatsAsReserved(any(), any())).thenReturn(2);
			when(seatRepository.releaseSeatsByIds(any())).thenReturn(2);

			// Act & Assert
			IllegalStateException exception = assertThrows(
					IllegalStateException.class,
					() -> seatService.reserveSeats(eventId, customerName, seatCount));

			assertEquals("Failed to reserve all requested seats. Only 2 of 3 were reserved.", exception.getMessage());
			// Verify rollback was attempted
			verify(seatRepository).releaseSeatsByIds(any());
		}
	}

	@Test
	void cancelReservation_WhenSuccessful_ShouldReleaseSeatsAndDeleteReservation() {
		// Arrange
		UUID reservationId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();

		com.otis.model.entity.Reservation reservation = new com.otis.model.entity.Reservation(reservationId, eventId,
				"John", 2, "pending", null);

		when(reservationRepository.findById(reservationId)).thenReturn(reservation);
		when(reservationSeatRepository.findByReservationId(reservationId)).thenReturn(Collections.emptyList());
		when(seatRepository.releaseSeatsByIds(any())).thenReturn(2);
		when(reservationSeatRepository.deleteByReservationId(reservationId)).thenReturn(1);
		when(reservationRepository.cancel(reservationId)).thenReturn(true);

		// Act
		boolean result = seatService.cancelReservation(reservationId);

		// Assert
		assertTrue(result);
		verify(seatRepository).releaseSeatsByIds(any());
		verify(reservationRepository).cancel(reservationId);
	}

	@Test
	void cancelReservation_WhenNotFound_ShouldReturnFalse() {
		// Arrange
		UUID reservationId = UUID.randomUUID();

		when(reservationRepository.findById(reservationId)).thenReturn(null);

		// Act
		boolean result = seatService.cancelReservation(reservationId);

		// Assert
		org.junit.jupiter.api.Assertions.assertFalse(result);
	}

	@Test
	void findByEventId_ShouldReturnPaginatedSeats() {
		// Arrange
		UUID eventId = UUID.randomUUID();
		int page = 0;
		int size = 20;

		Seat seat1 = createTestSeat(eventId, "A1");
		Seat seat2 = createTestSeat(eventId, "A2");

		var expectedResponse = new com.otis.model.entity.PageResponse<>(
				List.of(seat1, seat2), page, size, 2, 1, true, true);
		when(seatRepository.findByEventId(page, size, eventId)).thenReturn(expectedResponse);

		// Act
		var result = seatService.findByEventId(page, size, eventId);

		// Assert
		assertNotNull(result);
		assertEquals(2, result.content().size());
		assertEquals(2, result.totalElements());
	}

	private Seat createTestSeat(UUID eventId, String seatNumber) {
		return new Seat(
				UUID.randomUUID(),
				eventId,
				seatNumber,
				"available",
				null,
				null,
				0,
				false,
				null);
	}
}
