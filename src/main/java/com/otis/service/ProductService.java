package com.otis.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.Product;
import com.otis.repository.ProductRepository;
import com.otis.util.BulkheadUtils;

import io.github.resilience4j.bulkhead.Bulkhead;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductService {
	private final ProductRepository productRepository;
	private final Bulkhead bulkhead;

	public ProductService(ProductRepository productRepository, Bulkhead databaseBulkhead) {
		this.productRepository = productRepository;
		this.bulkhead = databaseBulkhead;
	}

	public List<Product> findByFilters(UUID id, String name, UUID companyId, String companyName) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> productRepository.findByFilters(id, name, companyId, companyName), "findByFilters");
	}
}
