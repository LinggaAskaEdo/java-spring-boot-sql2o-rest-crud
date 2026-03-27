package com.otis.model.dto;

import java.util.List;

import com.otis.model.entity.Product;

public record Response(List<Product> products) {
}
