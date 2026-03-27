package com.otis.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.Company;
import com.otis.repository.CompanyRepository;
import com.otis.util.BulkheadUtils;

import io.github.resilience4j.bulkhead.Bulkhead;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CompanyService {
	private final CompanyRepository repository;
	private final Bulkhead bulkhead;

	public CompanyService(CompanyRepository repository, Bulkhead databaseBulkhead) {
		this.repository = repository;
		this.bulkhead = databaseBulkhead;
	}

	public List<Company> findByFilters(UUID id, String name) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> repository.findByFilters(id, name), "findByFilters");
	}
}
