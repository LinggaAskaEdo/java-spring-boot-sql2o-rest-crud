package com.otis.util;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils {
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private JsonUtils() {
	}

	public static ObjectMapper getMapper() {
		return MAPPER;
	}
}
