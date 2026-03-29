package com.otis.repository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.otis.model.entity.Company;
import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Product;
import com.otis.model.entity.Tutorial;

@ExtendWith(MockitoExtension.class)
class RepositoryTest {
	@Mock
	private Sql2o sql2o;

	@Mock
	private Connection connection;

	@Mock
	private Query query;

	private ProductRepository productRepository;
	private CompanyRepository companyRepository;
	private TutorialRepository tutorialRepository;

	@BeforeEach
	@SuppressWarnings("unused")
	void setUp() {
		productRepository = new ProductRepository(sql2o);
		companyRepository = new CompanyRepository(sql2o);
		tutorialRepository = new TutorialRepository(sql2o);
	}

	@Test
	void ProductRepository_ShouldCreateInstance() {
		assertNotNull(productRepository);
	}

	@Test
	void CompanyRepository_ShouldCreateInstance() {
		assertNotNull(companyRepository);
	}

	@Test
	void TutorialRepository_ShouldCreateInstance() {
		assertNotNull(tutorialRepository);
	}

	@Test
	void ProductRepository_FindByFilters_ShouldReturnPageResponse() {
		UUID productId = UUID.randomUUID();
		UUID companyId = UUID.randomUUID();
		Product product = new Product(productId, "Test Product", companyId, "Test Company", List.of());

		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Product.class)).thenReturn(List.of(product));
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(1);

		PageResponse<Product> result = productRepository.findByFilters(0, 10, null, null, null, null);

		assertNotNull(result);
		assertEquals(1, result.totalElements());
		assertEquals(1, result.content().size());
		assertEquals("Test Product", result.content().get(0).name());
	}

	@Test
	void ProductRepository_FindByFilters_WithAllParams_ShouldReturnFilteredResult() {
		UUID productId = UUID.randomUUID();
		UUID companyId = UUID.randomUUID();
		Product product = new Product(productId, "Spring Boot", companyId, "TechCorp", List.of());

		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Product.class)).thenReturn(List.of(product));
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(1);

		PageResponse<Product> result = productRepository.findByFilters(0, 10, productId, "Spring", companyId,
				"TechCorp");

		assertNotNull(result);
		assertEquals(1, result.totalElements());
		assertEquals("Spring Boot", result.content().get(0).name());
	}

	@Test
	void ProductRepository_FindByFilters_ShouldReturnEmptyList() {
		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Product.class)).thenReturn(List.of());
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(0);

		PageResponse<Product> result = productRepository.findByFilters(0, 10, null, null, null, null);

		assertNotNull(result);
		assertEquals(0, result.totalElements());
		assertEquals(0, result.content().size());
	}

	@Test
	void ProductRepository_FindByFilters_WithPagination_ShouldReturnCorrectPage() {
		UUID companyId = UUID.randomUUID();
		Product product1 = new Product(UUID.randomUUID(), "Product 1", companyId, "Company", List.of());
		Product product2 = new Product(UUID.randomUUID(), "Product 2", companyId, "Company", List.of());

		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Product.class)).thenReturn(List.of(product1, product2));
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(10);

		PageResponse<Product> result = productRepository.findByFilters(1, 2, null, null, null, null);

		assertNotNull(result);
		assertEquals(10, result.totalElements());
		assertEquals(2, result.content().size());
		assertEquals(5, result.totalPages());
		assertEquals(1, result.page());
	}

	@Test
	void CompanyRepository_FindByFilters_ShouldReturnPageResponse() {
		UUID companyId = UUID.randomUUID();
		Company company = new Company(companyId, "Tech Corp");

		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Company.class)).thenReturn(List.of(company));
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(1);

		PageResponse<Company> result = companyRepository.findByFilters(0, 10, null, null);

		assertNotNull(result);
		assertEquals(1, result.totalElements());
		assertEquals("Tech Corp", result.content().get(0).name());
	}

	@Test
	void CompanyRepository_FindByFilters_WithIdAndName_ShouldReturnFilteredResult() {
		UUID companyId = UUID.randomUUID();
		Company company = new Company(companyId, "Acme Inc");

		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Company.class)).thenReturn(List.of(company));
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(1);

		PageResponse<Company> result = companyRepository.findByFilters(0, 10, companyId, "Acme");

		assertNotNull(result);
		assertEquals(1, result.totalElements());
		assertEquals("Acme Inc", result.content().get(0).name());
	}

	@Test
	void CompanyRepository_FindByFilters_ShouldReturnEmptyList() {
		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Company.class)).thenReturn(List.of());
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(0);

		PageResponse<Company> result = companyRepository.findByFilters(0, 10, null, null);

		assertNotNull(result);
		assertEquals(0, result.totalElements());
		assertEquals(0, result.content().size());
	}

	@Test
	void CompanyRepository_FindByFilters_WithPagination_ShouldReturnCorrectPage() {
		Company company1 = new Company(UUID.randomUUID(), "Company A");
		Company company2 = new Company(UUID.randomUUID(), "Company B");

		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Company.class)).thenReturn(List.of(company1, company2));
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(8);

		PageResponse<Company> result = companyRepository.findByFilters(2, 2, null, null);

		assertNotNull(result);
		assertEquals(8, result.totalElements());
		assertEquals(4, result.totalPages());
		assertEquals(2, result.page());
	}

	@Test
	void TutorialRepository_FindByFilters_ShouldReturnPageResponse() {
		UUID tutorialId = UUID.randomUUID();
		Tutorial tutorial = new Tutorial(tutorialId, "Spring Tutorial", "Learn Spring", true);

		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Tutorial.class)).thenReturn(List.of(tutorial));
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(1);

		PageResponse<Tutorial> result = tutorialRepository.findByFilters(0, 10, null, null, null, null);

		assertNotNull(result);
		assertEquals(1, result.totalElements());
		assertEquals("Spring Tutorial", result.content().get(0).title());
	}

	@Test
	void TutorialRepository_FindByFilters_WithAllParams_ShouldReturnFilteredResult() {
		UUID tutorialId = UUID.randomUUID();
		Tutorial tutorial = new Tutorial(tutorialId, "Java Guide", "Complete Java", true);

		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Tutorial.class)).thenReturn(List.of(tutorial));
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(1);

		PageResponse<Tutorial> result = tutorialRepository.findByFilters(0, 10, tutorialId, "Java", "Complete", true);

		assertNotNull(result);
		assertEquals(1, result.totalElements());
		assertEquals("Java Guide", result.content().get(0).title());
	}

	@Test
	void TutorialRepository_FindByFilters_WithPublishedFilter_ShouldReturnOnlyPublished() {
		Tutorial published = new Tutorial(UUID.randomUUID(), "Published", "Desc", true);

		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Tutorial.class)).thenReturn(List.of(published));
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(1);

		PageResponse<Tutorial> result = tutorialRepository.findByFilters(0, 10, null, null, null, true);

		assertNotNull(result);
		assertEquals(1, result.totalElements());
		assertEquals(true, result.content().get(0).published());
	}

	@Test
	void TutorialRepository_FindByFilters_ShouldReturnEmptyList() {
		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Tutorial.class)).thenReturn(List.of());
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(0);

		PageResponse<Tutorial> result = tutorialRepository.findByFilters(0, 10, null, null, null, null);

		assertNotNull(result);
		assertEquals(0, result.totalElements());
		assertEquals(0, result.content().size());
	}

	@Test
	void TutorialRepository_FindByFilters_WithPagination_ShouldReturnCorrectPage() {
		Tutorial tutorial1 = new Tutorial(UUID.randomUUID(), "Tutorial 1", "Desc 1", true);
		Tutorial tutorial2 = new Tutorial(UUID.randomUUID(), "Tutorial 2", "Desc 2", false);

		when(sql2o.open()).thenReturn(connection);
		when(connection.createQuery(anyString())).thenReturn(query);
		when(query.addParameter(anyString(), any(Object.class))).thenReturn(query);
		when(query.executeAndFetch(Tutorial.class)).thenReturn(List.of(tutorial1, tutorial2));
		when(query.executeAndFetchFirst(Integer.class)).thenReturn(12);

		PageResponse<Tutorial> result = tutorialRepository.findByFilters(2, 2, null, null, null, null);

		assertNotNull(result);
		assertEquals(12, result.totalElements());
		assertEquals(6, result.totalPages());
		assertEquals(2, result.page());
		assertEquals(2, result.content().size());
	}
}
