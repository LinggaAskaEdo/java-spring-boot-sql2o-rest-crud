package com.otis.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.otis.model.entity.Product;
import com.otis.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

	private MockMvc mockMvc;

	@Mock
	private ProductService productService;

	@InjectMocks
	private ProductController productController;

	private UUID companyId;
	private UUID productId;
	private Product product;
	private PageResponse<Product> pageResponse;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(productController).build();
		companyId = UUID.randomUUID();
		productId = UUID.randomUUID();
		product = new Product(productId, "Test Product", companyId, "Test Company", Collections.emptyList());
		pageResponse = new PageResponse<>(List.of(product), 0, 10, 1, 1, true, true);
	}

	@Test
	void getAllProducts_WithDefaultParams_ReturnsOk() throws Exception {
		when(productService.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/products")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content[0].name").value("Test Product"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void getAllProducts_WithPagination_ReturnsOk() throws Exception {
		when(productService.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/products")
						.param("page", "0")
						.param("size", "10")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void getAllProducts_WithIdFilter_ReturnsFilteredResult() throws Exception {
		when(productService.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/products")
						.param("id", productId.toString())
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(productId.toString()));
	}

	@Test
	void getAllProducts_WithNameFilter_ReturnsFilteredResult() throws Exception {
		when(productService.findByFilters(anyInt(), anyInt(), any(), anyString(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/products")
						.param("name", "Test Product")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Test Product"));
	}

	@Test
	void getAllProducts_WithCompanyFilter_ReturnsFilteredResult() throws Exception {
		when(productService.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/products")
						.param("company", companyId.toString())
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	void getAllProducts_WithCompanyNameFilter_ReturnsFilteredResult() throws Exception {
		when(productService.findByFilters(anyInt(), anyInt(), any(), any(), any(), anyString()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/products")
						.param("companyName", "Test Company")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	void getAllProducts_WithEmptyResult_ReturnsEmptyList() throws Exception {
		PageResponse<Product> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);
		when(productService.findByFilters(anyInt(), anyInt(), any(), any(), any(), any()))
				.thenReturn(emptyResponse);

		mockMvc.perform(get("/api/products")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void getAllProducts_WithAllFilters_ReturnsFilteredResult() throws Exception {
		when(productService.findByFilters(anyInt(), anyInt(), any(), anyString(), any(), anyString()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/products")
						.param("page", "0")
						.param("size", "10")
						.param("id", productId.toString())
						.param("name", "Test Product")
						.param("company", companyId.toString())
						.param("companyName", "Test Company")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Test Product"));
	}
}
