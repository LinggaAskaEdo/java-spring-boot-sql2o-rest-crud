package com.otis.config;

import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseThrottleConfig {
	@Value("${spring.datasource.hikari.maximum-pool-size:10}")
	private int maximumPoolSize;

	@Value("${spring.datasource.hikari.throttle-percentage:98}")
	private int throttlePercentage;

	@Bean
	public Semaphore databaseSemaphore() {
		return new Semaphore((maximumPoolSize * throttlePercentage) / 100);
	}
}
