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

import com.otis.model.Company;
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
	public ResponseEntity<List<Company>> getAllCompanies(
			@RequestParam(required = false) UUID id,
			@RequestParam(required = false) String name) {
		List<Company> companies = service.findByFilters(id, name);
		if (companies.isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}

		return new ResponseEntity<>(companies, HttpStatus.OK);
	}
}
