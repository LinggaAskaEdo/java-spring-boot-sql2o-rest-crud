package com.otis.config;

import java.io.IOException;
import java.security.SecureRandom;

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
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String traceId = request.getHeader("X-Trace-Id");
		if (traceId == null || traceId.isBlank()) {
			traceId = generateTraceId();
		}

		String threadId = String.valueOf(Thread.currentThread().threadId());

		long startTime = System.currentTimeMillis();

		response.setHeader("X-Trace-Id", traceId);

		log.info("START | traceId={} | method={} | uri={} | threadId={}",
				traceId, request.getMethod(), request.getRequestURI(), threadId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			long processTime = System.currentTimeMillis() - startTime;
			int status = response.getStatus();

			log.info("END | traceId={} | method={} | uri={} | status={} | processTime={}ms | threadId={}",
					traceId, request.getMethod(), request.getRequestURI(), status, processTime, threadId);
		}
	}

	private String generateTraceId() {
		long timestamp = System.currentTimeMillis();
		int random = RANDOM.nextInt(0xFFFFFF);

		return Long.toHexString(timestamp) + Integer.toHexString(random);
	}
}
