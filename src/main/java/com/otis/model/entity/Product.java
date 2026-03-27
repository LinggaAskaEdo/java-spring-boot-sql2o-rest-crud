package com.otis.model.entity;

import java.util.List;
import java.util.UUID;

public record Product(UUID id, String name, UUID companyID, String companyName, List<Company> companies) {
}
