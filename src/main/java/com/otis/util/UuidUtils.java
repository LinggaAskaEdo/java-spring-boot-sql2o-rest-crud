package com.otis.util;

import java.util.UUID;

import com.otis.exception.BadRequestException;

import xyz.block.uuidv7.MonotonicUUIDv7;

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

	public static UUID randomUuidV7() {
		return MonotonicUUIDv7.generate();
	}
}
