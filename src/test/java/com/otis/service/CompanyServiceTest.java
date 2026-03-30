package com.otis.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.otis.model.entity.Company;
import com.otis.model.entity.PageResponse;
import com.otis.repository.CompanyRepository;

class CompanyServiceTest {
	private CompanyRepository repository;
	private CompanyService companyService;

	@BeforeEach
	@SuppressWarnings("unused")
	void setUp() {
		repository = mock(CompanyRepository.class);
		companyService = new CompanyService(repository);
	}

	@Test
	void findByFilters_ReturnsPageResponse() {
		UUID companyId = UUID.randomUUID();
		Company company = new Company(companyId, "Test Company");
		PageResponse<Company> expectedResponse = new PageResponse<>(List.of(company), 0, 10, 1, 1, true, true);

		when(repository.findByFilters(anyInt(), anyInt(), any(), any()))
				.thenReturn(expectedResponse);

		PageResponse<Company> result = companyService.findByFilters(0, 10, null, null);

		assertEquals(expectedResponse, result);
		assertEquals(1, result.content().size());
		assertEquals("Test Company", result.content().get(0).name());
	}

	@Test
	void findByFilters_WithIdFilter_ReturnsFilteredResult() {
		UUID companyId = UUID.randomUUID();
		Company company = new Company(companyId, "Test Company");
		PageResponse<Company> expectedResponse = new PageResponse<>(List.of(company), 0, 10, 1, 1, true, true);

		when(repository.findByFilters(eq(0), eq(10), eq(companyId), any()))
				.thenReturn(expectedResponse);

		PageResponse<Company> result = companyService.findByFilters(0, 10, companyId, null);

		assertEquals(expectedResponse, result);
		assertEquals(companyId, result.content().get(0).id());
	}

	@Test
	void findByFilters_WithNameFilter_ReturnsFilteredResult() {
		UUID companyId = UUID.randomUUID();
		Company company = new Company(companyId, "Test Company");
		PageResponse<Company> expectedResponse = new PageResponse<>(List.of(company), 0, 10, 1, 1, true, true);

		when(repository.findByFilters(eq(0), eq(10), any(), eq("Test")))
				.thenReturn(expectedResponse);

		PageResponse<Company> result = companyService.findByFilters(0, 10, null, "Test");

		assertEquals(expectedResponse, result);
		assertEquals("Test Company", result.content().get(0).name());
	}

	@Test
	void findByFilters_ReturnsEmptyList() {
		PageResponse<Company> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);

		when(repository.findByFilters(anyInt(), anyInt(), any(), any()))
				.thenReturn(emptyResponse);

		PageResponse<Company> result = companyService.findByFilters(0, 10, null, null);

		assertEquals(0, result.totalElements());
		assertEquals(0, result.content().size());
	}

	@Test
	void findByFilters_WithPagination_ReturnsCorrectPage() {
		Company company = new Company(UUID.randomUUID(), "Test Company");
		PageResponse<Company> pageResponse = new PageResponse<>(List.of(company), 1, 5, 10, 2, false, false);

		when(repository.findByFilters(eq(1), eq(5), any(), any()))
				.thenReturn(pageResponse);

		PageResponse<Company> result = companyService.findByFilters(1, 5, null, null);

		assertEquals(1, result.page());
		assertEquals(5, result.size());
		assertEquals(10, result.totalElements());
		assertEquals(2, result.totalPages());
		assertEquals(false, result.first());
		assertEquals(false, result.last());
	}
}
