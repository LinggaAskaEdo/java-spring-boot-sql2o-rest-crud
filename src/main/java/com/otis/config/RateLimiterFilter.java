package com.otis.config;

import java.io.IOException;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.otis.exception.RateLimitExceededException;
import com.otis.exception.UnauthorizedException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimiterFilter extends OncePerRequestFilter {
	private static final Logger log = LoggerFactory.getLogger(RateLimiterFilter.class);
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
			throw new UnauthorizedException("Missing required header: " + API_KEY_HEADER);
		}

		String tier = rateLimiterProperties.getTierByApiKey(apiKey);
		if (tier == null) {
			throw new UnauthorizedException("Invalid API key");
		}

		String rateLimiterKey = "rate_limiter:tier:" + tier;
		RRateLimiter rateLimiter = redissonClient.getRateLimiter(rateLimiterKey);

		if (!rateLimiter.tryAcquire()) {
			log.warn("Rate limit exceeded for API key: {}", apiKey);
			throw new RateLimitExceededException("Rate limit exceeded for tier: " + tier);
		}

		filterChain.doFilter(request, response);
	}
}
