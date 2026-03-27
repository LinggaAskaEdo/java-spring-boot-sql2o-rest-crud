package com.otis.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.Tutorial;
import com.otis.repository.TutorialRepository;
import com.otis.util.BulkheadUtils;

import io.github.resilience4j.bulkhead.Bulkhead;

@Service
public class TutorialService {
	private final TutorialRepository repository;
	private final Bulkhead bulkhead;

	public TutorialService(TutorialRepository repository, Bulkhead databaseBulkhead) {
		this.repository = repository;
		this.bulkhead = databaseBulkhead;
	}

	public List<Tutorial> findAll() {
		return BulkheadUtils.withBulkhead(bulkhead, repository::findAll, "findAll");
	}

	public List<Tutorial> findByTitleContaining(String title) {
		return BulkheadUtils.withBulkhead(bulkhead,
				() -> repository.findByTitleContaining(title), "findByTitleContaining");
	}

	public Optional<Tutorial> findById(UUID id) {
		return BulkheadUtils.withBulkhead(bulkhead, () -> repository.findById(id), "findById");
	}
}
