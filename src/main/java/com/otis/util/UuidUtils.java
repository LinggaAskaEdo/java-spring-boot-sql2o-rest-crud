package com.otis.util;

import java.util.UUID;

import com.otis.exception.BadRequestException;

public final class UuidUtils {
	private UuidUtils() {
	}

	public static UUID parseUUID(String id) {
		try {
			return UUID.fromString(id);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Invalid UUID format: " + id);
		}
	}
}
