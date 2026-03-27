package com.otis.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.otis.model.entity.Company;
import com.otis.model.entity.PageResponse;
import com.otis.service.CompanyService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class CompanyController {
	private final CompanyService service;

	public CompanyController(CompanyService service) {
		this.service = service;
	}

	@GetMapping("/companies")
	public ResponseEntity<PageResponse<Company>> getAllCompanies(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(required = false) UUID id,
			@RequestParam(required = false) String name) {
		return ResponseEntity.ok(service.findByFilters(page, size, id, name));
	}
}
