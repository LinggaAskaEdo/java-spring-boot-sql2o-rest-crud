package com.otis.config;

import java.time.Duration;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RateLimiterKeyInitializer implements ApplicationRunner {
	private static final String KEY_PREFIX = "rate_limiter:tier:";

	private final RedissonClient redissonClient;
	private final RateLimiterProperties rateLimiterProperties;

	public RateLimiterKeyInitializer(RedissonClient redissonClient, RateLimiterProperties rateLimiterProperties) {
		this.redissonClient = redissonClient;
		this.rateLimiterProperties = rateLimiterProperties;
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info("Starting rate limiter initialization, tiers from config: {}",
				rateLimiterProperties.getTiers().keySet());
		log.info("API keys from config: {}", rateLimiterProperties.getApiKeys());

		if (rateLimiterProperties.getTiers().isEmpty()) {
			log.warn("No tiers found in configuration! Rate limiters will not be initialized.");
			return;
		}

		rateLimiterProperties.getTiers().forEach((tierName, tierConfig) -> {
			String key = KEY_PREFIX + tierName;
			RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

			log.info("Checking rate limiter for tier '{}', key: {}, exists: {}", tierName, key, rateLimiter.isExists());

			if (!rateLimiter.isExists()) {
				rateLimiter.setRate(RateType.OVERALL, tierConfig.getMaxRequests(), Duration.ofMinutes(1));
				log.info("Initialized Redis rate limiter for tier '{}' with max requests: {} per second", tierName,
						tierConfig.getMaxRequests());
			} else {
				log.info("Redis rate limiter for tier '{}' already exists", tierName);
			}
		});

		log.info("Rate limiter initialization completed");
	}
}
