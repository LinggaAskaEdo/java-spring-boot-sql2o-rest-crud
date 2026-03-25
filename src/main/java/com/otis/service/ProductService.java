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

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class ProductService {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private final ProductRepository productRepository;
	private final CompanyRepository companyRepository;

	public ProductService(ProductRepository productRepository, CompanyRepository companyRepository) {
		this.productRepository = productRepository;
		this.companyRepository = companyRepository;
	}

	public List<Product> findAll() {
		return productRepository.findAll();
	}

	public List<Product> findByNameContaining(String title) {
		return productRepository.findByNameContaining(title);
	}

	public List<Product> findByCompanyName(String companyName) {
		Company company = companyRepository.getCompanyByName(companyName);

		if (null == company) {
			throw new ResourceNotFoundException("Not found Company with name = " + companyName);
		}

		return productRepository.findProductByCompanyID(company.getId());
	}

	public Response getReportData() {
		Response response = new Response();
		Map<Product, List<Company>> responseMap = new HashMap<>();

		List<Map<String, Object>> results = productRepository.getReportData();

		if (!results.isEmpty()) {
			log.info("Data is not empty !!!");
			for (Map<String, Object> map : results) {
				Product product = Product.builder()
						.id(UUID.fromString(map.get("product_id").toString()))
						.name(map.get("product_name").toString())
						.build();

				Company company = Company.builder()
						.id(UUID.fromString(map.get("company_id").toString()))
						.name(map.get("company_name").toString())
						.build();

				if (responseMap.containsKey(product)) {
					List<Company> currentCompanies = responseMap.get(product);

					if (!currentCompanies.contains(company)) {
						currentCompanies.add(company);
					}

					responseMap.put(product, currentCompanies);
				} else {
					List<Company> companies = new ArrayList<>();
					companies.add(company);
					responseMap.put(product, companies);
				}
			}

			List<Product> products = new ArrayList<>();

			for (Map.Entry<Product, List<Company>> data : responseMap.entrySet()) {
				Product productKey = data.getKey();
				List<Company> companyValue = data.getValue();
				productKey.setCompanies(companyValue);
				products.add(productKey);
			}

			response.setProducts(products);

			String json = objectMapper.writeValueAsString(response);
			log.info("Results: {}", json);
		} else {
			log.info("Data is empty !!!");
		}

		return response;
	}
}
