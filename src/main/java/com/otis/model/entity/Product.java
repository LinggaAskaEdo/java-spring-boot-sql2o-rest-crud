package com.otis.model.entity;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.CacheStrategy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(cacheStrategy = CacheStrategy.LAZY)
public class Product {
	private UUID id;
	private String name;
	private UUID companyID;
	private String companyName;
	private List<Company> companies;
}
