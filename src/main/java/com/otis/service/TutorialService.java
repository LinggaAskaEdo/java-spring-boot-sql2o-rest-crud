package com.otis.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.otis.model.Tutorial;
import com.otis.repository.TutorialRepository;

@Service
public class TutorialService {
	private final TutorialRepository repository;

	public TutorialService(TutorialRepository repository) {
		this.repository = repository;
	}

	public List<Tutorial> findAll() {
		return repository.findAll();
	}

	public List<Tutorial> findByTitleContaining(String title) {
		return repository.findByTitleContaining(title);
	}

	public Optional<Tutorial> findById(UUID id) {
		return repository.findById(id);
	}
}
