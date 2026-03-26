package com.otis.util;

import java.security.SecureRandom;

public final class RandomUtils {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private RandomUtils() {
	}

	public static SecureRandom getSecureRandom() {
		return SECURE_RANDOM;
	}
}
