package com.otis.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Reservation;
import com.otis.model.entity.Seat;
import com.otis.service.SeatService;

@ExtendWith(MockitoExtension.class)
class SeatControllerTest {
	private MockMvc mockMvc;

	@Mock
	private SeatService seatService;

	@InjectMocks
	private SeatController seatController;

	private UUID eventId;
	private UUID seatId;
	private UUID reservationId;
	private Seat seat;
	private Reservation reservation;
	private PageResponse<Seat> pageResponse;

	@BeforeEach
	@SuppressWarnings("unused")
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(seatController).build();
		eventId = UUID.randomUUID();
		seatId = UUID.randomUUID();
		reservationId = UUID.randomUUID();
		seat = new Seat(seatId, eventId, "A1", false, null);
		reservation = new Reservation(reservationId, eventId, "John Doe", 3);
		pageResponse = new PageResponse<>(List.of(seat), 0, 20, 1, 1, true, true);
	}

	@Test
	void getSeatsByEvent_WithDefaultParams_ReturnsOk() throws Exception {
		when(seatService.findByEventId(anyInt(), anyInt(), any())).thenReturn(pageResponse);

		mockMvc.perform(get("/api/events/{eventId}/seats", eventId)
				.contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content[0].seatNumber").value("A1"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void getSeatsByEvent_WithPagination_ReturnsOk() throws Exception {
		when(seatService.findByEventId(anyInt(), anyInt(), any())).thenReturn(pageResponse);

		mockMvc.perform(get("/api/events/{eventId}/seats", eventId)
				.param("page", "0")
				.param("size", "20")
				.contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void getSeatsByEvent_WithEmptyResult_ReturnsEmptyList() throws Exception {
		PageResponse<Seat> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 20, 0, 0, true, true);
		when(seatService.findByEventId(anyInt(), anyInt(), any())).thenReturn(emptyResponse);

		mockMvc.perform(get("/api/events/{eventId}/seats", eventId)
				.contentType(APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void reserveSeats_WhenSuccessful_ReturnsOk() throws Exception {
		when(seatService.reserveSeats(any(), anyString(), anyInt())).thenReturn(reservation);

		mockMvc.perform(post("/api/events/{eventId}/reserve", eventId)
				.contentType(APPLICATION_JSON)
				.content("{\"customerName\": \"John Doe\", \"seatCount\": 3}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(reservationId.toString()))
				.andExpect(jsonPath("$.customerName").value("John Doe"))
				.andExpect(jsonPath("$.seatCount").value(3));
	}

	@Test
	void reserveSeats_WhenNotEnoughSeats_ReturnsBadRequest() throws Exception {
		when(seatService.reserveSeats(any(), anyString(), anyInt())).thenReturn(null);

		mockMvc.perform(post("/api/events/{eventId}/reserve", eventId)
				.contentType(APPLICATION_JSON)
				.content("{\"customerName\": \"John Doe\", \"seatCount\": 100}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cancelReservation_WhenSuccessful_ReturnsNoContent() throws Exception {
		when(seatService.cancelReservation(reservationId)).thenReturn(true);

		mockMvc.perform(post("/api/reservations/{reservationId}/cancel", reservationId)
				.contentType(APPLICATION_JSON))
				.andExpect(status().isNoContent());
	}

	@Test
	void cancelReservation_WhenNotFound_ReturnsNotFound() throws Exception {
		when(seatService.cancelReservation(any())).thenReturn(false);

		mockMvc.perform(post("/api/reservations/{reservationId}/cancel", UUID.randomUUID())
				.contentType(APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}
}
