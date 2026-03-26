package com.otis.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Service;

import com.otis.exception.DatabaseThrottleException;
import com.otis.exception.ResourceNotFoundException;
import com.otis.model.Company;
import com.otis.model.Product;
import com.otis.model.Response;
import com.otis.repository.CompanyRepository;
import com.otis.repository.ProductRepository;
import com.otis.util.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductService {
	private final ProductRepository productRepository;
	private final CompanyRepository companyRepository;
	private final Semaphore databaseSemaphore;

	public ProductService(ProductRepository productRepository, CompanyRepository companyRepository,
			Semaphore databaseSemaphore) {
		this.productRepository = productRepository;
		this.companyRepository = companyRepository;
		this.databaseSemaphore = databaseSemaphore;
	}

	public List<Product> findAll() {
		try {
			databaseSemaphore.acquire();
			return productRepository.findAll();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DatabaseThrottleException("Interrupted while waiting for database throttle", e);
		} finally {
			databaseSemaphore.release();
		}
	}

	public List<Product> findByNameContaining(String title) {
		try {
			databaseSemaphore.acquire();
			return productRepository.findByNameContaining(title);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DatabaseThrottleException("Interrupted while waiting for database throttle", e);
		} finally {
			databaseSemaphore.release();
		}
	}

	public List<Product> findByCompanyName(String companyName) {
		Company company;

		try {
			databaseSemaphore.acquire();
			company = companyRepository.getCompanyByName(companyName);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DatabaseThrottleException("Interrupted while waiting for database throttle", e);
		} finally {
			databaseSemaphore.release();
		}

		if (null == company) {
			throw new ResourceNotFoundException("Not found Company with name = " + companyName);
		}

		try {
			databaseSemaphore.acquire();
			return productRepository.findProductByCompanyID(company.getId());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DatabaseThrottleException("Interrupted while waiting for database throttle", e);
		} finally {
			databaseSemaphore.release();
		}
	}

	public Response getReportData() {
		Response response = new Response();
		List<Map<String, Object>> results = fetchReportData();

		if (results.isEmpty()) {
			log.info("Data is empty !!!");
			return response;
		}

		log.info("Data is not empty !!!");
		Map<Product, List<Company>> responseMap = buildResponseMap(results);
		List<Product> products = buildProductList(responseMap);
		response.setProducts(products);
		logReportResults(response);

		return response;
	}

	private List<Map<String, Object>> fetchReportData() {
		try {
			databaseSemaphore.acquire();
			return productRepository.getReportData();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DatabaseThrottleException("Interrupted while waiting for database throttle", e);
		} finally {
			databaseSemaphore.release();
		}
	}

	private Map<Product, List<Company>> buildResponseMap(List<Map<String, Object>> results) {
		Map<Product, List<Company>> responseMap = new HashMap<>();
		for (Map<String, Object> map : results) {
			Product product = Product.builder()
					.id(UUID.fromString(map.get("product_id").toString()))
					.name(map.get("product_name").toString())
					.build();

			Company company = Company.builder()
					.id(UUID.fromString(map.get("company_id").toString()))
					.name(map.get("company_name").toString())
					.build();

			addCompanyToProduct(responseMap, product, company);
		}
		return responseMap;
	}

	private void addCompanyToProduct(Map<Product, List<Company>> responseMap, Product product, Company company) {
		if (responseMap.containsKey(product)) {
			List<Company> currentCompanies = responseMap.get(product);
			if (!currentCompanies.contains(company)) {
				currentCompanies.add(company);
			}
		} else {
			List<Company> companies = new ArrayList<>();
			companies.add(company);
			responseMap.put(product, companies);
		}
	}

	private List<Product> buildProductList(Map<Product, List<Company>> responseMap) {
		List<Product> products = new ArrayList<>();
		for (Map.Entry<Product, List<Company>> data : responseMap.entrySet()) {
			Product productKey = data.getKey();
			productKey.setCompanies(data.getValue());
			products.add(productKey);
		}
		return products;
	}

	private void logReportResults(Response response) {
		try {
			String json = JsonUtils.getMapper().writeValueAsString(response);
			log.info("Results: {}", json);
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			log.error("Failed to serialize response to JSON", e);
		}
	}
}
