package com.otis.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@ExtendWith(MockitoExtension.class)
class ControllerExceptionHandlerTest {
	private MockMvc mockMvc;
	private ControllerExceptionHandler exceptionHandler;

	@RestController
	@RequestMapping("/test")
	static class TestController {

		@GetMapping("/bad-request")
		public String triggerBadRequest() {
			throw new BadRequestException("Bad request message");
		}

		@GetMapping("/not-found")
		public String triggerNotFound() {
			throw new ResourceNotFoundException("Resource not found message");
		}

		@GetMapping("/bulkhead-full")
		public String triggerBulkheadFull() {
			throw new BulkheadFullException("Bulkhead full message");
		}

		@GetMapping("/unauthorized")
		public String triggerUnauthorized() {
			throw new UnauthorizedException("Unauthorized message");
		}

		@GetMapping("/rate-limit")
		public String triggerRateLimit() {
			throw new RateLimitExceededException("Rate limit exceeded message");
		}

		@GetMapping("/global")
		public String triggerGlobal() {
			throw new RuntimeException("Internal server error message");
		}
	}

	@Mock
	private WebRequest webRequest;

	@BeforeEach
	@SuppressWarnings("unused")
	void setUp() {
		exceptionHandler = new ControllerExceptionHandler();
		lenient().when(webRequest.getDescription(false)).thenReturn("test-description");
		TestController testController = new TestController();
		mockMvc = MockMvcBuilders.standaloneSetup(testController)
				.setControllerAdvice(exceptionHandler)
				.build();
	}

	@Test
	void handleBadRequestException_ShouldReturn400() throws Exception {
		mockMvc.perform(get("/test/bad-request")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.statusCode").value(400))
				.andExpect(jsonPath("$.message").value("Bad request message"))
				.andExpect(jsonPath("$.description").exists());
	}

	@Test
	void handleResourceNotFoundException_ShouldReturn404() throws Exception {
		mockMvc.perform(get("/test/not-found")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.statusCode").value(404))
				.andExpect(jsonPath("$.message").value("Resource not found message"))
				.andExpect(jsonPath("$.description").exists());
	}

	@Test
	void handleBulkheadFullException_ShouldReturn429() throws Exception {
		mockMvc.perform(get("/test/bulkhead-full")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.statusCode").value(429))
				.andExpect(jsonPath("$.message").value("Too many requests. Please try again later."))
				.andExpect(jsonPath("$.description").exists());
	}

	@Test
	void handleUnauthorizedException_ShouldReturn401() throws Exception {
		mockMvc.perform(get("/test/unauthorized")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.statusCode").value(401))
				.andExpect(jsonPath("$.message").value("Unauthorized message"))
				.andExpect(jsonPath("$.description").exists());
	}

	@Test
	void handleRateLimitExceededException_ShouldReturn429() throws Exception {
		mockMvc.perform(get("/test/rate-limit")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.statusCode").value(429))
				.andExpect(jsonPath("$.message").value("Rate limit exceeded. Please try again later."))
				.andExpect(jsonPath("$.description").exists());
	}

	@Test
	void handleGlobalException_ShouldReturn500() throws Exception {
		mockMvc.perform(get("/test/global")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.statusCode").value(500))
				.andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."))
				.andExpect(jsonPath("$.description").exists());
	}

	@Test
	void badRequestException_Method_ShouldReturnErrorMessage() {
		BadRequestException ex = new BadRequestException("Test error");
		ErrorMessage result = exceptionHandler.badRequestException(ex, webRequest);

		assertNotNull(result);
		assertEquals(400, result.getStatusCode());
		assertEquals("Test error", result.getMessage());
	}

	@Test
	void resourceNotFoundException_Method_ShouldReturnErrorMessage() {
		ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
		ErrorMessage result = exceptionHandler.resourceNotFoundException(ex, webRequest);

		assertNotNull(result);
		assertEquals(404, result.getStatusCode());
		assertEquals("Not found", result.getMessage());
	}

	@Test
	void bulkheadFullException_Method_ShouldReturnGenericMessage() {
		BulkheadFullException ex = new BulkheadFullException("Original message");
		ErrorMessage result = exceptionHandler.bulkheadFullException(ex, webRequest);

		assertNotNull(result);
		assertEquals(429, result.getStatusCode());
		assertEquals("Too many requests. Please try again later.", result.getMessage());
	}

	@Test
	void unauthorizedException_Method_ShouldReturnErrorMessage() {
		UnauthorizedException ex = new UnauthorizedException("Access denied");
		ErrorMessage result = exceptionHandler.unauthorizedException(ex, webRequest);

		assertNotNull(result);
		assertEquals(401, result.getStatusCode());
		assertEquals("Access denied", result.getMessage());
	}

	@Test
	void rateLimitExceededException_Method_ShouldReturnErrorMessage() {
		RateLimitExceededException ex = new RateLimitExceededException("Rate exceeded");
		ErrorMessage result = exceptionHandler.rateLimitExceededException(ex, webRequest);

		assertNotNull(result);
		assertEquals(429, result.getStatusCode());
		assertEquals("Rate limit exceeded. Please try again later.", result.getMessage());
	}

	@Test
	void globalExceptionHandler_Method_ShouldReturnErrorMessage() {
		Exception ex = new Exception("Server error");
		ErrorMessage result = exceptionHandler.globalExceptionHandler(ex, webRequest);

		assertNotNull(result);
		assertEquals(500, result.getStatusCode());
		assertEquals("An unexpected error occurred. Please try again later.", result.getMessage());
	}
}
