package com.otis.model.entity;

import java.util.UUID;

public record Tutorial(UUID id, String title, String description, boolean published) {
}
