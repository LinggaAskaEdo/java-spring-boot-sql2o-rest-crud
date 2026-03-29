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

import com.otis.model.entity.Company;
import com.otis.model.entity.PageResponse;
import com.otis.service.CompanyService;

@ExtendWith(MockitoExtension.class)
class CompanyControllerTest {
	private MockMvc mockMvc;

	@Mock
	private CompanyService companyService;

	@InjectMocks
	private CompanyController companyController;

	private UUID companyId;
	private Company company;
	private PageResponse<Company> pageResponse;

	@BeforeEach
	@SuppressWarnings("unused")
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(companyController).build();
		companyId = UUID.randomUUID();
		company = new Company(companyId, "Test Company");
		pageResponse = new PageResponse<>(List.of(company), 0, 10, 1, 1, true, true);
	}

	@Test
	void getAllCompanies_WithDefaultParams_ReturnsOk() throws Exception {
		when(companyService.findByFilters(anyInt(), anyInt(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/companies")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content[0].name").value("Test Company"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void getAllCompanies_WithPagination_ReturnsOk() throws Exception {
		when(companyService.findByFilters(anyInt(), anyInt(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/companies")
				.param("page", "0")
				.param("size", "10")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void getAllCompanies_WithIdFilter_ReturnsFilteredResult() throws Exception {
		when(companyService.findByFilters(anyInt(), anyInt(), any(), any()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/companies")
				.param("id", companyId.toString())
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(companyId.toString()));
	}

	@Test
	void getAllCompanies_WithNameFilter_ReturnsFilteredResult() throws Exception {
		when(companyService.findByFilters(anyInt(), anyInt(), any(), anyString()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/companies")
				.param("name", "Test Company")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Test Company"));
	}

	@Test
	void getAllCompanies_WithEmptyResult_ReturnsEmptyList() throws Exception {
		PageResponse<Company> emptyResponse = new PageResponse<>(Collections.emptyList(), 0, 10, 0, 0, true, true);
		when(companyService.findByFilters(anyInt(), anyInt(), any(), any()))
				.thenReturn(emptyResponse);

		mockMvc.perform(get("/api/companies")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void getAllCompanies_WithAllFilters_ReturnsFilteredResult() throws Exception {
		when(companyService.findByFilters(anyInt(), anyInt(), any(), anyString()))
				.thenReturn(pageResponse);

		mockMvc.perform(get("/api/companies")
				.param("page", "0")
				.param("size", "10")
				.param("id", companyId.toString())
				.param("name", "Test Company")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Test Company"));
	}
}
