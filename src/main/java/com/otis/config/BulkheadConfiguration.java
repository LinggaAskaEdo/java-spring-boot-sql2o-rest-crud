package com.otis.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;

@Configuration
public class BulkheadConfiguration {
	@Value("${resilience4j.bulkhead.instances.database.max-concurrent-calls:29}")
	private int maxConcurrentCalls;

	@Value("${resilience4j.bulkhead.instances.database.max-wait-duration-ms:0}")
	private long maxWaitDurationMs;

	@Bean
	public Bulkhead databaseBulkhead() {
		BulkheadConfig config = BulkheadConfig.custom()
				.maxConcurrentCalls(maxConcurrentCalls)
				.maxWaitDuration(Duration.ofMillis(maxWaitDurationMs))
				.build();

		return Bulkhead.of("database", config);
	}
}
