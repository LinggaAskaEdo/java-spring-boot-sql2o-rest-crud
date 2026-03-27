package com.otis.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Product;
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

	public PageResponse<Product> findByFilters(int page, int size, UUID id, String name, UUID companyId,
			String companyName) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> productRepository.findByFilters(page, size, id, name, companyId, companyName), "findByFilters");
	}
}
