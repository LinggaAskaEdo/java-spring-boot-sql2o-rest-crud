package com.otis.util;

import java.util.function.Supplier;

import com.otis.exception.BulkheadFullException;

import io.github.resilience4j.bulkhead.Bulkhead;

public final class BulkheadUtils {
	private BulkheadUtils() {
	}

	public static <T> T withBulkhead(Bulkhead bulkhead, Supplier<T> action, String methodName) {
		if (!bulkhead.tryAcquirePermission()) {
			throw new BulkheadFullException("Bulkhead is full in " + methodName);
		}

		try {
			return action.get();
		} finally {
			bulkhead.onComplete();
		}
	}
}
