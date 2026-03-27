package com.otis.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.otis.exception.ResourceNotFoundException;
import com.otis.model.Tutorial;
import com.otis.service.TutorialService;
import com.otis.util.UuidUtils;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class TutorialController {
	private final TutorialService service;

	public TutorialController(TutorialService service) {
		this.service = service;
	}

	@GetMapping("/tutorials")
	public ResponseEntity<List<Tutorial>> getAllTutorials(@RequestParam(required = false) String title) {
		List<Tutorial> tutorials = new ArrayList<>();

		if (title == null)
			tutorials.addAll(service.findAll());
		else
			tutorials.addAll(service.findByTitleContaining(title));

		if (tutorials.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}

		return new ResponseEntity<>(tutorials, HttpStatus.OK);
	}

	@GetMapping("/tutorials/{id}")
	public ResponseEntity<Tutorial> getTutorialById(@PathVariable String id) {
		UUID uuid = UuidUtils.parseUUID(id);

		Tutorial tutorial = service.findById(uuid)
				.orElseThrow(() -> new ResourceNotFoundException("Not found Tutorial with id = " + id));

		return new ResponseEntity<>(tutorial, HttpStatus.OK);
	}
}
