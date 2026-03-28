package com.otis.config;

import java.time.Duration;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterKeyInitializer implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(RateLimiterKeyInitializer.class);
	private static final String KEY_PREFIX = "rate_limiter:tier:";

	private final RedissonClient redissonClient;
	private final RateLimiterProperties rateLimiterProperties;

	public RateLimiterKeyInitializer(RedissonClient redissonClient, RateLimiterProperties rateLimiterProperties) {
		this.redissonClient = redissonClient;
		this.rateLimiterProperties = rateLimiterProperties;
	}

	@Override
	public void run(ApplicationArguments args) {
		rateLimiterProperties.getTiers().forEach((tierName, tierConfig) -> {
			String key = KEY_PREFIX + tierName;
			RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

			if (!rateLimiter.isExists()) {
				rateLimiter.setRate(RateType.OVERALL, tierConfig.getMaxRequests(), Duration.ofSeconds(1));
				log.info("Initialized Redis rate limiter key '{}' with max requests: {}", tierName,
						tierConfig.getMaxRequests());
			} else {
				log.info("Redis rate limiter key '{}' already exists, skipping initialization", tierName);
			}
		});
	}
}
