package com.otis.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Tutorial;
import com.otis.service.TutorialService;

@CrossOrigin(origins = "${api.cors.allowed-origins:*}")
@RestController
@RequestMapping("/api/v1")
public class TutorialController {
	private final TutorialService service;
	private final int maxPageSize;
	private final int defaultPageSize;

	public TutorialController(TutorialService service,
			@Value("${api.pagination.max-page-size:100}") int maxPageSize,
			@Value("${api.pagination.default-page-size:10}") int defaultPageSize) {
		this.service = service;
		this.maxPageSize = maxPageSize;
		this.defaultPageSize = defaultPageSize;
	}

	@GetMapping("/tutorials")
	public ResponseEntity<PageResponse<Tutorial>> getAllTutorials(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(required = false, defaultValue = "") String size,
			@RequestParam(required = false) UUID id,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String description,
			@RequestParam(required = false) Boolean published) {

		// Apply pagination limits
		int effectiveSize = parseSize(size, defaultPageSize);

		return ResponseEntity.ok(service.findByFilters(page, effectiveSize, id, title, description, published));
	}

	private int parseSize(String size, int defaultSize) {
		if (size == null || size.isBlank()) {
			return defaultSize;
		}

		try {
			int parsed = Integer.parseInt(size);
			return (parsed <= 0) ? defaultSize : Math.min(parsed, maxPageSize);
		} catch (NumberFormatException e) {
			return defaultSize;
		}
	}
}
