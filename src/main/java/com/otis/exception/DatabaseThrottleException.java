package com.otis.exception;

public class DatabaseThrottleException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public DatabaseThrottleException(String msg) {
		super(msg);
	}

	public DatabaseThrottleException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
