package com.otis.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Product;
import com.otis.service.ProductService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class ProductController {
	private final ProductService service;

	public ProductController(ProductService service) {
		this.service = service;
	}

	@GetMapping("/products")
	public ResponseEntity<PageResponse<Product>> getAllProducts(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) UUID id,
			@RequestParam(required = false) String name,
			@RequestParam(required = false) UUID company,
			@RequestParam(required = false) String companyName) {
		return ResponseEntity.ok(service.findByFilters(page, size, id, name, company, companyName));
	}
}
