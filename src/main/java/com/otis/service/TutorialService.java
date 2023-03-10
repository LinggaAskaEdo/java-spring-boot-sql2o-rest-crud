package com.otis.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.otis.model.Tutorial;
import com.otis.repository.TutorialRepository;

@Service
public class TutorialService {
    private final TutorialRepository repository;

    @Autowired
    public TutorialService(TutorialRepository repository) {
        this.repository = repository;
    }

    public List<Tutorial> findAll() {
        return repository.findAll();
    }

    public List<Tutorial> findByTitleContaining(String title) {
        return repository.findByTitleContaining(title);
    }

    public Optional<Tutorial> findById(long id) {
        return repository.findById(id);
    }
}
