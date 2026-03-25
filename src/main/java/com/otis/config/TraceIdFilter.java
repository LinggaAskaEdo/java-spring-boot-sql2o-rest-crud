package com.otis.config;

import java.io.IOException;
import java.security.SecureRandom;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
	private static final SecureRandom RANDOM = new SecureRandom();

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

		try {
			filterChain.doFilter(request, response);
		} finally {
			long processTime = System.currentTimeMillis() - startTime;
			int status = response.getStatus();

			MDC.put("event", "END");
			MDC.put("status", String.valueOf(status));
			MDC.put("processTime", String.valueOf(processTime) + " ms");

			log.info("Request completed");

			MDC.clear();
		}
	}

	private String generateTraceId() {
		long timestamp = System.currentTimeMillis();
		int random = RANDOM.nextInt(0xFFFFFF);
		return Long.toHexString(timestamp) + Integer.toHexString(random);
	}
}