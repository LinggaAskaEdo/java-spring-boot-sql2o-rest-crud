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

	public static UUID bytesToUuid(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			if (i == 4 || i == 6 || i == 8 || i == 10) {
				sb.append('-');
			}
			sb.append(String.format("%02x", bytes[i]));
		}
		return UUID.fromString(sb.toString());
	}
}
