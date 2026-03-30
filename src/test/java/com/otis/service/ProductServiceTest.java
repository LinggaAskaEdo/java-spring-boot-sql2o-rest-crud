package com.otis.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Product;
import com.otis.repository.ProductRepository;

class ProductServiceTest {
	private ProductRepository productRepository;
	private ProductService productService;

	@BeforeEach
	@SuppressWarnings("unused")
	void setUp() {
		productRepository = mock(ProductRepository.class);
		productService = new ProductService(productRepository);
	}

	@Test
	void findByFilters_ReturnsPageResponse() {
		UUID companyId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		Product product = new Product(productId, "Test Product", companyId, "Test Company", Collections.emptyList());
		PageResponse<Product> expectedResponse = new PageResponse<>(List.of(product), 0, 10, 1, 1, true, true);

		when(productRepository.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(expectedResponse);

		PageResponse<Product> result = productService.findByFilters(0, 10, null, null, null, null);

		assertEquals(expectedResponse, result);
		assertEquals(1, result.content().size());
		assertEquals("Test Product", result.content().get(0).name());
	}

	@Test
	void findByFilters_WithAllParams_ReturnsFilteredResult() {
		UUID filterCompanyId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		Product product = new Product(productId, "Test Product", filterCompanyId, "Test Company",
				Collections.emptyList());
		PageResponse<Product> expectedResponse = new PageResponse<>(List.of(product), 0, 10, 1, 1, true, true);

		when(productRepository.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(expectedResponse);

		PageResponse<Product> result = productService.findByFilters(0, 10, productId, "Test", filterCompanyId,
				"Company");

		assertEquals(expectedResponse, result);
		assertEquals("Test Product", result.content().get(0).name());
	}

	@Test
	void findByFilters_ReturnsEmptyList() {
		PageResponse<Product> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);

		when(productRepository.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(emptyResponse);

		PageResponse<Product> result = productService.findByFilters(0, 10, null, null, null, null);

		assertEquals(0, result.totalElements());
		assertEquals(0, result.content().size());
		assertEquals(true, result.first());
		assertEquals(true, result.last());
	}

	@Test
	void findByFilters_WithPagination_ReturnsCorrectPage() {
		UUID companyId = UUID.randomUUID();
		Product product = new Product(UUID.randomUUID(), "Test Product", companyId, "Test Company",
				Collections.emptyList());
		PageResponse<Product> pageResponse = new PageResponse<>(List.of(product), 2, 5, 10, 2, false, true);

		when(productRepository.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(pageResponse);

		PageResponse<Product> result = productService.findByFilters(2, 5, null, null, null, null);

		assertEquals(2, result.page());
		assertEquals(5, result.size());
		assertEquals(10, result.totalElements());
		assertEquals(2, result.totalPages());
		assertEquals(false, result.first());
		assertEquals(true, result.last());
	}
}
