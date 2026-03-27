package com.otis.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.otis.model.PageResponse;
import com.otis.model.Tutorial;
import com.otis.service.TutorialService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class TutorialController {
	private final TutorialService service;

	public TutorialController(TutorialService service) {
		this.service = service;
	}

	@GetMapping("/tutorials")
	public ResponseEntity<PageResponse<Tutorial>> getAllTutorials(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) UUID id,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String description,
			@RequestParam(required = false) Boolean published) {
		return ResponseEntity.ok(service.findByFilters(page, size, id, title, description, published));
	}
}
