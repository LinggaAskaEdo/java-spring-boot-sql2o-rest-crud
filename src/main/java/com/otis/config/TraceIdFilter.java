package com.otis.config;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.otis.util.RandomUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain)
			throws ServletException, IOException {

		String traceId = request.getHeader("X-Trace-Id");
		if (traceId == null || traceId.isBlank()) {
			traceId = generateTraceId();
		}

		response.setHeader("X-Trace-Id", traceId);

		// Use threadId() instead of deprecated getId()
		String threadId = String.valueOf(Thread.currentThread().threadId());
		String method = request.getMethod();
		String uri = request.getRequestURI();

		MDC.put("event", "START");
		MDC.put("traceId", traceId);
		MDC.put("method", method);
		MDC.put("uri", uri);
		MDC.put("threadId", threadId);

		long startTime = System.currentTimeMillis();
		log.info("Request started");

		// Wrap the response to capture the body
		LoggingResponseWrapper loggingWrapper = new LoggingResponseWrapper(response);

		try {
			filterChain.doFilter(request, loggingWrapper);
		} finally {
			long processTime = System.currentTimeMillis() - startTime;
			int status = loggingWrapper.getStatus();

			// Only log the body for non‑2xx responses
			if (status < 200 || status >= 300) {
				byte[] responseBody = loggingWrapper.getCapturedBody();
				if (responseBody.length > 0) {
					String charset = loggingWrapper.getCharacterEncoding();
					if (charset == null)
						charset = "UTF-8";
					String bodyString = new String(responseBody, charset);

					// Truncate very long bodies to avoid log flooding
					final int MAX_BODY_LENGTH = 2000;
					if (bodyString.length() > MAX_BODY_LENGTH) {
						bodyString = bodyString.substring(0, MAX_BODY_LENGTH) + "... [truncated]";
					}

					log.warn("Non-2xx response (status {}): body = {}", status, bodyString);
				} else {
					log.warn("Non-2xx response (status {}) with empty body", status);
				}
			}

			MDC.put("event", "END");
			MDC.put("status", String.valueOf(status));
			MDC.put("processTime", String.valueOf(processTime) + " ms");
			log.info("Request completed");

			MDC.clear();
		}
	}

	private String generateTraceId() {
		long timestamp = System.currentTimeMillis();
		int random = RandomUtils.getSecureRandom().nextInt(0xFFFFFF);

		return Long.toHexString(timestamp) + Integer.toHexString(random);
	}
}