package com.otis.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
	public ResponseEntity<List<Tutorial>> getAllTutorials(
			@RequestParam(required = false) UUID id,
			@RequestParam(required = false) String title,
			@RequestParam(required = false) String description,
			@RequestParam(required = false) Boolean published) {
		List<Tutorial> tutorials = service.findByFilters(id, title, description, published);
		if (tutorials.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}

		return new ResponseEntity<>(tutorials, HttpStatus.OK);
	}
}
