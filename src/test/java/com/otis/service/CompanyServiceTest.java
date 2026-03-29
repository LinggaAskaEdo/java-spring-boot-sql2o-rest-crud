package com.otis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.otis.model.entity.Company;
import com.otis.model.entity.PageResponse;
import com.otis.repository.CompanyRepository;
import com.otis.util.BulkheadUtils;

import io.github.resilience4j.bulkhead.Bulkhead;

class CompanyServiceTest {

	private CompanyRepository repository;
	private Bulkhead bulkhead;
	private CompanyService companyService;

	@BeforeEach
	void setUp() {
		repository = mock(CompanyRepository.class);
		bulkhead = mock(Bulkhead.class);
		companyService = new CompanyService(repository, bulkhead);
	}

	@Test
	void findByFilters_ReturnsPageResponse() {
		UUID companyId = UUID.randomUUID();
		Company company = new Company(companyId, "Test Company");
		PageResponse<Company> expectedResponse = new PageResponse<>(List.of(company), 0, 10, 1, 1, true, true);

		when(repository.findByFilters(anyInt(), anyInt(), any(), any()))
				.thenReturn(expectedResponse);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			PageResponse<Company> result = companyService.findByFilters(0, 10, null, null);

			assertEquals(expectedResponse, result);
			assertEquals(1, result.content().size());
			assertEquals("Test Company", result.content().get(0).name());
		}
	}

	@Test
	void findByFilters_WithIdFilter_ReturnsFilteredResult() {
		UUID companyId = UUID.randomUUID();
		Company company = new Company(companyId, "Test Company");
		PageResponse<Company> expectedResponse = new PageResponse<>(List.of(company), 0, 10, 1, 1, true, true);

		when(repository.findByFilters(eq(0), eq(10), eq(companyId), any()))
				.thenReturn(expectedResponse);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			PageResponse<Company> result = companyService.findByFilters(0, 10, companyId, null);

			assertEquals(expectedResponse, result);
			assertEquals(companyId, result.content().get(0).id());
		}
	}

	@Test
	void findByFilters_WithNameFilter_ReturnsFilteredResult() {
		UUID companyId = UUID.randomUUID();
		Company company = new Company(companyId, "Test Company");
		PageResponse<Company> expectedResponse = new PageResponse<>(List.of(company), 0, 10, 1, 1, true, true);

		when(repository.findByFilters(eq(0), eq(10), any(), eq("Test")))
				.thenReturn(expectedResponse);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			PageResponse<Company> result = companyService.findByFilters(0, 10, null, "Test");

			assertEquals(expectedResponse, result);
			assertEquals("Test Company", result.content().get(0).name());
		}
	}

	@Test
	void findByFilters_ReturnsEmptyList() {
		PageResponse<Company> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);

		when(repository.findByFilters(anyInt(), anyInt(), any(), any()))
				.thenReturn(emptyResponse);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			PageResponse<Company> result = companyService.findByFilters(0, 10, null, null);

			assertEquals(0, result.totalElements());
			assertEquals(0, result.content().size());
		}
	}

	@Test
	void findByFilters_WithPagination_ReturnsCorrectPage() {
		Company company = new Company(UUID.randomUUID(), "Test Company");
		PageResponse<Company> pageResponse = new PageResponse<>(List.of(company), 1, 5, 10, 2, false, false);

		when(repository.findByFilters(eq(1), eq(5), any(), any()))
				.thenReturn(pageResponse);

		try (var mockedBulkheadUtils = mockStatic(BulkheadUtils.class)) {
			mockedBulkheadUtils.when(() -> BulkheadUtils.withBulkhead(any(Bulkhead.class), any(Supplier.class), anyString()))
					.thenAnswer(invocation -> {
						Supplier<?> supplier = invocation.getArgument(1);
						return supplier.get();
					});

			PageResponse<Company> result = companyService.findByFilters(1, 5, null, null);

			assertEquals(1, result.page());
			assertEquals(5, result.size());
			assertEquals(10, result.totalElements());
			assertEquals(2, result.totalPages());
			assertEquals(false, result.first());
			assertEquals(false, result.last());
		}
	}
}
