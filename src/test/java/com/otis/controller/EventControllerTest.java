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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.otis.model.entity.Event;
import com.otis.model.entity.PageResponse;
import com.otis.service.EventService;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {
	private MockMvc mockMvc;

	@Mock
	private EventService eventService;

	@InjectMocks
	private EventController eventController;

	private UUID eventId;
	private Event event;
	private PageResponse<Event> pageResponse;

	@BeforeEach
	@SuppressWarnings("unused")
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(eventController).build();
		eventId = UUID.randomUUID();
		event = new Event(eventId, "Test Event", "Test Venue");
		pageResponse = new PageResponse<>(List.of(event), 0, 10, 1, 1, true, true);
	}

	@Test
	void getAllEvents_WithDefaultParams_ReturnsOk() throws Exception {
		when(eventService.findByFilters(anyInt(), anyInt(), any(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/events")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content[0].name").value("Test Event"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void getAllEvents_WithPagination_ReturnsOk() throws Exception {
		when(eventService.findByFilters(anyInt(), anyInt(), any(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/events")
				.param("page", "0")
				.param("size", "10")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void getAllEvents_WithIdFilter_ReturnsFilteredResult() throws Exception {
		when(eventService.findByFilters(anyInt(), anyInt(), any(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/events")
				.param("id", eventId.toString())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(eventId.toString()));
	}

	@Test
	void getAllEvents_WithNameFilter_ReturnsFilteredResult() throws Exception {
		when(eventService.findByFilters(anyInt(), anyInt(), any(), anyString(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/events")
				.param("name", "Test Event")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Test Event"));
	}

	@Test
	void getAllEvents_WithVenueFilter_ReturnsFilteredResult() throws Exception {
		when(eventService.findByFilters(anyInt(), anyInt(), any(), any(), anyString()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/events")
				.param("venue", "Test Venue")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].venue").value("Test Venue"));
	}

	@Test
	void getAllEvents_WithEmptyResult_ReturnsEmptyList() throws Exception {
		PageResponse<Event> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);
		when(eventService.findByFilters(anyInt(), anyInt(), any(), any(), any()))
				.thenReturn(emptyResponse);

		mockMvc.perform(get("/api/events")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void getEventById_WhenExists_ReturnsOk() throws Exception {
		when(eventService.findById(eventId)).thenReturn(event);

		mockMvc.perform(get("/api/events/{id}", eventId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(eventId.toString()))
				.andExpect(jsonPath("$.name").value("Test Event"))
				.andExpect(jsonPath("$.venue").value("Test Venue"));
	}

	@Test
	void getEventById_WhenNotExists_ReturnsNotFound() throws Exception {
		when(eventService.findById(any())).thenReturn(null);

		mockMvc.perform(get("/api/events/{id}", UUID.randomUUID())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	void getAvailableSeats_ReturnsOk() throws Exception {
		when(eventService.getAvailableSeats(eventId)).thenReturn(50);
		when(eventService.getTotalSeats(eventId)).thenReturn(100);

		mockMvc.perform(get("/api/events/{id}/seats/available", eventId)
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(100))
				.andExpect(jsonPath("$.available").value(50));
	}
}
