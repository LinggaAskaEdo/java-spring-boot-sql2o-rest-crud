package com.otis.config;

import java.io.IOException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

	private static final Logger log = LogManager.getLogger(TraceIdFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String traceId = request.getHeader("X-Trace-Id");
		if (traceId == null || traceId.isBlank()) {
			traceId = UUID.randomUUID().toString();
		}

		String threadId = ThreadContext.get("threadId");
		if (threadId == null) {
			threadId = String.valueOf(Thread.currentThread().getId());
			ThreadContext.put("threadId", threadId);
		}

		long startTime = System.currentTimeMillis();

		ThreadContext.put("traceId", traceId);
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

			ThreadContext.remove("traceId");
		}
	}
}
