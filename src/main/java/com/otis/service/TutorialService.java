package com.otis.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.PageResponse;
import com.otis.model.Tutorial;
import com.otis.repository.TutorialRepository;
import com.otis.util.BulkheadUtils;

import io.github.resilience4j.bulkhead.Bulkhead;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TutorialService {
	private final TutorialRepository repository;
	private final Bulkhead bulkhead;

	public TutorialService(TutorialRepository repository, Bulkhead databaseBulkhead) {
		this.repository = repository;
		this.bulkhead = databaseBulkhead;
	}

	public PageResponse<Tutorial> findByFilters(int page, int size, UUID id, String title, String description,
			Boolean published) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> repository.findByFilters(page, size, id, title, description, published), "findByFilters");
	}
}
