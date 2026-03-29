package com.otis.exception;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ExceptionTest {
	@Test
	void BadRequestException_ShouldStoreMessage() {
		BadRequestException ex = new BadRequestException("Invalid input");
		assertEquals("Invalid input", ex.getMessage());
		assertTrue(ex instanceof RuntimeException);
	}

	@Test
	void ResourceNotFoundException_ShouldStoreMessage() {
		ResourceNotFoundException ex = new ResourceNotFoundException("Resource not found");
		assertEquals("Resource not found", ex.getMessage());
		assertTrue(ex instanceof RuntimeException);
	}

	@Test
	void UnauthorizedException_ShouldStoreMessage() {
		UnauthorizedException ex = new UnauthorizedException("Access denied");
		assertEquals("Access denied", ex.getMessage());
		assertTrue(ex instanceof RuntimeException);
	}

	@Test
	void BulkheadFullException_ShouldStoreMessage() {
		BulkheadFullException ex = new BulkheadFullException("Bulkhead full");
		assertEquals("Bulkhead full", ex.getMessage());
		assertTrue(ex instanceof RuntimeException);
	}

	@Test
	void RateLimitExceededException_ShouldStoreMessage() {
		RateLimitExceededException ex = new RateLimitExceededException("Rate limit exceeded");
		assertEquals("Rate limit exceeded", ex.getMessage());
		assertTrue(ex instanceof RuntimeException);
	}

	@Test
	void AllExceptions_ShouldHaveDefaultSerialVersionUID() throws Exception {
		Class<?>[] exceptionClasses = {
				BadRequestException.class,
				ResourceNotFoundException.class,
				BulkheadFullException.class
		};

		for (Class<?> clazz : exceptionClasses) {
			Constructor<?> constructor = clazz.getDeclaredConstructor(String.class);
			Exception ex = (Exception) constructor.newInstance("test");
			assertNotNull(ex);
			assertEquals("test", ex.getMessage());
		}
	}
}
