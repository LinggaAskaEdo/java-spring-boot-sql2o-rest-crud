package com.otis.exception;

public class BulkheadFullException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public BulkheadFullException(String message) {
		super(message);
	}
}
