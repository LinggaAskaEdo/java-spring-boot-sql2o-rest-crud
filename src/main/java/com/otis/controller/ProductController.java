package com.otis.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Product;
import com.otis.service.ProductService;

@CrossOrigin(origins = "${api.cors.allowed-origins:*}")
@RestController
@RequestMapping("/api/v1")
public class ProductController {
	private final ProductService service;
	private final int maxPageSize;
	private final int defaultPageSize;

	public ProductController(ProductService service,
			@Value("${api.pagination.max-page-size:100}") int maxPageSize,
			@Value("${api.pagination.default-page-size:10}") int defaultPageSize) {
		this.service = service;
		this.maxPageSize = maxPageSize;
		this.defaultPageSize = defaultPageSize;
	}

	@GetMapping("/products")
	public ResponseEntity<PageResponse<Product>> getAllProducts(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "") String size,
			@RequestParam(required = false) UUID id,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) UUID company,
			@RequestParam(required = false) String companyName) {

		// Apply pagination limits
		int effectiveSize = parseSize(size, defaultPageSize);

		return ResponseEntity.ok(service.findByFilters(page, effectiveSize, id, name, company, companyName));
	}

	private int parseSize(String size, int defaultSize) {
		if (size == null || size.isBlank()) {
			return defaultSize;
		}

		try {
			int parsed = Integer.parseInt(size);
			return (parsed <= 0) ? defaultSize : Math.min(parsed, maxPageSize);
		} catch (NumberFormatException e) {
			return defaultSize;
		}
	}
}
