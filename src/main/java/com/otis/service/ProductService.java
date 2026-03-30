package com.otis.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Product;
import com.otis.repository.ProductRepository;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductService {
	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Bulkhead(name = "database")
	@Retry(name = "database")
	public PageResponse<Product> findByFilters(int page, int size, UUID id, String name, UUID companyId,
			String companyName) {
		return productRepository.findByFilters(page, size, id, name, companyId, companyName);
	}
}
