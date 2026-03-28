package com.otis.config;

import java.io.IOException;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.otis.exception.ErrorMessage;
import com.otis.util.JsonUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimiterFilter extends OncePerRequestFilter {
	private static final String API_KEY_HEADER = "x-api-key";

	private final RedissonClient redissonClient;
	private final RateLimiterProperties rateLimiterProperties;

	public RateLimiterFilter(RedissonClient redissonClient, RateLimiterProperties rateLimiterProperties) {
		this.redissonClient = redissonClient;
		this.rateLimiterProperties = rateLimiterProperties;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String apiKey = request.getHeader(API_KEY_HEADER);

		if (apiKey == null || apiKey.isBlank()) {
			sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing required header: " + API_KEY_HEADER,
					request.getRequestURI());
			return;
		}

		String tier = rateLimiterProperties.getTierByApiKey(apiKey);
		if (tier == null) {
			sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid API key", request.getRequestURI());
			return;
		}

		String rateLimiterKey = "rate_limiter:tier:" + tier;
		RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimiterKey);

		log.debug("Rate limiter key: {}, isExists: {}", rateLimiterKey, rateLimiter.isExists());

		if (!rateLimiter.tryAcquire()) {
			log.warn("Rate limit exceeded for API key: {}", apiKey);
			sendErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS,
					"Rate limit exceeded for tier: " + tier, request.getRequestURI());
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message, String uri)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);

		ErrorMessage errorMessage = new ErrorMessage(
				status.value(),
				new java.util.Date(),
				message,
				"uri=" + uri);

		JsonUtils.getMapper().writeValue(response.getWriter(), errorMessage);
	}
}
