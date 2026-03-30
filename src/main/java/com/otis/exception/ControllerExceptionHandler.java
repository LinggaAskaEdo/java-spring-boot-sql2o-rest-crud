package com.otis.exception;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ControllerExceptionHandler {
	@ExceptionHandler(BadRequestException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ErrorMessage badRequestException(BadRequestException ex, WebRequest request) {
		return new ErrorMessage(
				HttpStatus.BAD_REQUEST.value(),
				new Date(),
				ex.getMessage(),
				request.getDescription(false));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	public ErrorMessage resourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
		return new ErrorMessage(
				HttpStatus.NOT_FOUND.value(),
				new Date(),
				ex.getMessage(),
				request.getDescription(false));
	}

	@ExceptionHandler(BulkheadFullException.class)
	@ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS)
	public ErrorMessage bulkheadFullException(BulkheadFullException ex, WebRequest request) {
		return new ErrorMessage(
				HttpStatus.TOO_MANY_REQUESTS.value(),
				new Date(),
				"Too many requests. Please try again later.",
				request.getDescription(false));
	}

	@ExceptionHandler(UnauthorizedException.class)
	@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
	public ErrorMessage unauthorizedException(UnauthorizedException ex, WebRequest request) {
		return new ErrorMessage(
				HttpStatus.UNAUTHORIZED.value(),
				new Date(),
				ex.getMessage(),
				request.getDescription(false));
	}

	@ExceptionHandler(RateLimitExceededException.class)
	@ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS)
	public ErrorMessage rateLimitExceededException(RateLimitExceededException ex, WebRequest request) {
		return new ErrorMessage(
				HttpStatus.TOO_MANY_REQUESTS.value(),
				new Date(),
				"Rate limit exceeded. Please try again later.",
				request.getDescription(false));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	public ErrorMessage handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
		StringBuilder errors = new StringBuilder("Validation failed: ");
		ex.getBindingResult().getAllErrors().forEach(error -> {
			if (error instanceof FieldError fieldError) {
				String fieldName = fieldError.getField();
				String defaultMessage = fieldError.getDefaultMessage();
				errors.append(fieldName != null ? fieldName : "field")
						.append(" ")
						.append(defaultMessage != null ? defaultMessage : "invalid")
						.append("; ");
			} else {
				String message = (error != null) ? error.getDefaultMessage() : null;
				errors.append(message != null ? message : "validation error").append("; ");
			}
		});

		return new ErrorMessage(
				HttpStatus.BAD_REQUEST.value(),
				new Date(),
				errors.toString(),
				request.getDescription(false));
	}

	@ExceptionHandler(IllegalStateException.class)
	@ResponseStatus(value = HttpStatus.CONFLICT)
	public ErrorMessage illegalStateException(IllegalStateException ex, WebRequest request) {
		log.warn("Illegal state: {}", ex.getMessage());
		return new ErrorMessage(
				HttpStatus.CONFLICT.value(),
				new Date(),
				ex.getMessage(),
				request.getDescription(false));
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	public ErrorMessage globalExceptionHandler(Exception ex, WebRequest request) {
		// Log the full exception for debugging
		log.error("Internal server error", ex);

		// Return generic message to client - don't expose internal details
		return new ErrorMessage(
				HttpStatus.INTERNAL_SERVER_ERROR.value(),
				new Date(),
				"An unexpected error occurred. Please try again later.",
				request.getDescription(false));
	}
}
