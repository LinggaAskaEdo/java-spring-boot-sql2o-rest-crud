package com.otis.model.entity;

import java.util.UUID;

public record Event(UUID id, String name, String venue) {
}
