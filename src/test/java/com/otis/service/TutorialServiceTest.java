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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Tutorial;
import com.otis.repository.TutorialRepository;

class TutorialServiceTest {
	private TutorialRepository repository;
	private TutorialService tutorialService;

	@BeforeEach
	@SuppressWarnings("unused")
	void setUp() {
		repository = mock(TutorialRepository.class);
		tutorialService = new TutorialService(repository);
	}

	@Test
	void findByFilters_ReturnsPageResponse() {
		UUID tutorialId = UUID.randomUUID();
		Tutorial tutorial = new Tutorial(tutorialId, "Test Tutorial", "Description", true);
		PageResponse<Tutorial> expectedResponse = new PageResponse<>(List.of(tutorial), 0, 10, 1, 1, true, true);

		when(repository.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(expectedResponse);

		PageResponse<Tutorial> result = tutorialService.findByFilters(0, 10, null, null, null, null);

		assertEquals(expectedResponse, result);
		assertEquals(1, result.content().size());
		assertEquals("Test Tutorial", result.content().get(0).title());
		assertTrue(result.content().get(0).published());
	}

	@Test
	void findByFilters_WithAllFilters_ReturnsFilteredResult() {
		UUID tutorialId = UUID.randomUUID();
		Tutorial tutorial = new Tutorial(tutorialId, "Spring Tutorial", "Learn Spring Boot", true);
		PageResponse<Tutorial> expectedResponse = new PageResponse<>(List.of(tutorial), 0, 10, 1, 1, true, true);

		when(repository.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(expectedResponse);

		PageResponse<Tutorial> result = tutorialService.findByFilters(0, 10, tutorialId, "Spring", "Spring Boot",
				true);

		assertEquals(expectedResponse, result);
		assertEquals("Spring Tutorial", result.content().get(0).title());
	}

	@Test
	void findByFilters_WithPublishedFilter_ReturnsOnlyPublished() {
		UUID tutorialId = UUID.randomUUID();
		Tutorial tutorial = new Tutorial(tutorialId, "Published Tutorial", "Description", true);
		PageResponse<Tutorial> expectedResponse = new PageResponse<>(List.of(tutorial), 0, 10, 1, 1, true, true);

		when(repository.findByFilters(anyInt(), anyInt(), any(), any(), any(), anyBoolean()))
				.thenReturn(expectedResponse);

		PageResponse<Tutorial> result = tutorialService.findByFilters(0, 10, null, null, null, true);

		assertEquals(expectedResponse, result);
		assertTrue(result.content().get(0).published());
	}

	@Test
	void findByFilters_ReturnsEmptyList() {
		PageResponse<Tutorial> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);

		when(repository.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(emptyResponse);

		PageResponse<Tutorial> result = tutorialService.findByFilters(0, 10, null, null, null, null);

		assertEquals(0, result.totalElements());
		assertEquals(0, result.content().size());
		assertTrue(result.first());
		assertTrue(result.last());
	}

	@Test
	void findByFilters_WithPagination_ReturnsCorrectPage() {
		Tutorial tutorial1 = new Tutorial(UUID.randomUUID(), "Tutorial 1", "Desc 1", true);
		Tutorial tutorial2 = new Tutorial(UUID.randomUUID(), "Tutorial 2", "Desc 2", false);
		PageResponse<Tutorial> pageResponse = new PageResponse<>(List.of(tutorial1, tutorial2), 1, 2, 5, 3, false,
				false);

		when(repository.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(pageResponse);

		PageResponse<Tutorial> result = tutorialService.findByFilters(1, 2, null, null, null, null);

		assertEquals(1, result.page());
		assertEquals(2, result.size());
		assertEquals(5, result.totalElements());
		assertEquals(3, result.totalPages());
		assertEquals(2, result.content().size());
		assertFalse(result.first());
		assertFalse(result.last());
	}

	@Test
	void findByFilters_WithUnpublishedFilter_ReturnsUnpublishedTutorials() {
		UUID tutorialId = UUID.randomUUID();
		Tutorial tutorial = new Tutorial(tutorialId, "Unpublished Tutorial", "Description", false);
		PageResponse<Tutorial> expectedResponse = new PageResponse<>(List.of(tutorial), 0, 10, 1, 1, true, true);

		when(repository.findByFilters(anyInt(), anyInt(), any(), any(), any(), anyBoolean()))
				.thenReturn(expectedResponse);

		PageResponse<Tutorial> result = tutorialService.findByFilters(0, 10, null, null, null, false);

		assertEquals(expectedResponse, result);
		assertFalse(result.content().get(0).published());
	}
}
