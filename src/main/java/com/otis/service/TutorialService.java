package com.otis.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Tutorial;
import com.otis.repository.TutorialRepository;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TutorialService {
	private final TutorialRepository repository;

	public TutorialService(TutorialRepository repository) {
		this.repository = repository;
	}

	@Bulkhead(name = "database")
	@Retry(name = "database")
	public PageResponse<Tutorial> findByFilters(int page, int size, UUID id, String title, String description,
			Boolean published) {
		return repository.findByFilters(page, size, id, title, description, published);
	}
}
