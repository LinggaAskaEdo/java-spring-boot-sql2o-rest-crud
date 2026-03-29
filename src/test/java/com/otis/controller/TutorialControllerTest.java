package com.otis.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Tutorial;
import com.otis.service.TutorialService;

@ExtendWith(MockitoExtension.class)
class TutorialControllerTest {

	private MockMvc mockMvc;

	@Mock
	private TutorialService tutorialService;

	@InjectMocks
	private TutorialController tutorialController;

	private UUID tutorialId;
	private Tutorial tutorial;
	private PageResponse<Tutorial> pageResponse;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(tutorialController).build();
		tutorialId = UUID.randomUUID();
		tutorial = new Tutorial(tutorialId, "Test Tutorial", "Test Description", true);
		pageResponse = new PageResponse<>(List.of(tutorial), 0, 10, 1, 1, true, true);
	}

	@Test
	void getAllTutorials_WithDefaultParams_ReturnsOk() throws Exception {
		when(tutorialService.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/tutorials")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content[0].title").value("Test Tutorial"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void getAllTutorials_WithPagination_ReturnsOk() throws Exception {
		when(tutorialService.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/tutorials")
						.param("page", "0")
						.param("size", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void getAllTutorials_WithIdFilter_ReturnsFilteredResult() throws Exception {
		when(tutorialService.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/tutorials")
						.param("id", tutorialId.toString())
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(tutorialId.toString()));
	}

	@Test
	void getAllTutorials_WithTitleFilter_ReturnsFilteredResult() throws Exception {
		when(tutorialService.findByFilters(anyInt(), anyInt(), any(), anyString(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/tutorials")
						.param("title", "Test Tutorial")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].title").value("Test Tutorial"));
	}

	@Test
	void getAllTutorials_WithDescriptionFilter_ReturnsFilteredResult() throws Exception {
		when(tutorialService.findByFilters(anyInt(), anyInt(), any(), any(), anyString(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/tutorials")
						.param("description", "Test Description")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].description").value("Test Description"));
	}

	@Test
	void getAllTutorials_WithPublishedFilter_ReturnsFilteredResult() throws Exception {
		when(tutorialService.findByFilters(anyInt(), anyInt(), any(), any(), any(), anyBoolean()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/tutorials")
						.param("published", "true")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].published").value(true));
	}

	@Test
	void getAllTutorials_WithEmptyResult_ReturnsEmptyList() throws Exception {
		PageResponse<Tutorial> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);
		when(tutorialService.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(emptyResponse);

		mockMvc.perform(get("/api/tutorials")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void getAllTutorials_WithAllFilters_ReturnsFilteredResult() throws Exception {
		when(tutorialService.findByFilters(anyInt(), anyInt(), any(), anyString(), anyString(), anyBoolean()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/tutorials")
						.param("page", "0")
						.param("size", "10")
						.param("id", tutorialId.toString())
						.param("title", "Test Tutorial")
						.param("description", "Test Description")
						.param("published", "true")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].title").value("Test Tutorial"));
	}
}
