package com.otis.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Service;

import com.otis.exception.DatabaseThrottleException;
import com.otis.model.Tutorial;
import com.otis.repository.TutorialRepository;

@Service
public class TutorialService {
	private final TutorialRepository repository;
	private final Semaphore databaseSemaphore;

	public TutorialService(TutorialRepository repository, Semaphore databaseSemaphore) {
		this.repository = repository;
		this.databaseSemaphore = databaseSemaphore;
	}

	public List<Tutorial> findAll() {
		try {
			databaseSemaphore.acquire();
			return repository.findAll();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DatabaseThrottleException("Interrupted while waiting for database throttle", e);
		} finally {
			databaseSemaphore.release();
		}
	}

	public List<Tutorial> findByTitleContaining(String title) {
		try {
			databaseSemaphore.acquire();
			return repository.findByTitleContaining(title);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DatabaseThrottleException("Interrupted while waiting for database throttle", e);
		} finally {
			databaseSemaphore.release();
		}
	}

	public Optional<Tutorial> findById(UUID id) {
		try {
			databaseSemaphore.acquire();
			return repository.findById(id);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DatabaseThrottleException("Interrupted while waiting for database throttle", e);
		} finally {
			databaseSemaphore.release();
		}
	}
}
