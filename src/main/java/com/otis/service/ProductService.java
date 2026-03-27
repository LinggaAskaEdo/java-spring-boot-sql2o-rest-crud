package com.otis.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.exception.ResourceNotFoundException;
import com.otis.model.Company;
import com.otis.model.Product;
import com.otis.model.Response;
import com.otis.repository.CompanyRepository;
import com.otis.repository.ProductRepository;
import com.otis.util.BulkheadUtils;
import com.otis.util.JsonUtils;

import io.github.resilience4j.bulkhead.Bulkhead;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductService {
	private final ProductRepository productRepository;
	private final CompanyRepository companyRepository;
	private final Bulkhead bulkhead;

	public ProductService(ProductRepository productRepository, CompanyRepository companyRepository,
			Bulkhead databaseBulkhead) {
		this.productRepository = productRepository;
		this.companyRepository = companyRepository;
		this.bulkhead = databaseBulkhead;
	}

	public List<Product> findAll() {
		return BulkheadUtils.withBulkhead(bulkhead, productRepository::findAll, "findAll");
	}

	public List<Product> findByNameContaining(String title) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> productRepository.findByNameContaining(title), "findByNameContaining");
	}

	public List<Product> findByCompanyName(String companyName) {
		Company company = BulkheadUtils.withBulkhead(bulkhead,
				() -> companyRepository.getCompanyByName(companyName), "findByCompanyName");

		if (company == null) {
			throw new ResourceNotFoundException("Not found Company with name = " + companyName);
		}

		return BulkheadUtils.withBulkhead(bulkhead,
				() -> productRepository.findProductByCompanyID(company.getId()), "findByCompanyName");
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
		return BulkheadUtils.withBulkhead(bulkhead, productRepository::getReportData, "fetchReportData");
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
