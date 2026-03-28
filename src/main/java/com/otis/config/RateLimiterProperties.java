package com.otis.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {
	private Map<String, TierConfig> tiers = new HashMap<>();
	private Map<String, String> apiKeys = new HashMap<>();

	public String getTierByApiKey(String apiKey) {
		return apiKeys.get(apiKey);
	}

	@Data
	public static class TierConfig {
		private int maxRequests;
	}
}
