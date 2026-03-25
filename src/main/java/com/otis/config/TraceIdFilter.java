package com.otis.config;

import java.io.IOException;
import java.util.UUID;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String traceId = request.getHeader("X-Trace-Id");
		if (traceId == null || traceId.isBlank()) {
			traceId = UUID.randomUUID().toString();
		}

		ThreadContext.put("traceId", traceId);
		response.setHeader("X-Trace-Id", traceId);

		try {
			filterChain.doFilter(request, response);
		} finally {
			ThreadContext.remove("traceId");
		}
	}
}
