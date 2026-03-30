package com.otis.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.entity.Company;
import com.otis.model.entity.PageResponse;
import com.otis.repository.CompanyRepository;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CompanyService {
	private final CompanyRepository repository;

	public CompanyService(CompanyRepository repository) {
		this.repository = repository;
	}

	@Bulkhead(name = "database")
	@Retry(name = "database")
	public PageResponse<Company> findByFilters(int page, int size, UUID id, String name) {
		return repository.findByFilters(page, size, id, name);
	}
}
