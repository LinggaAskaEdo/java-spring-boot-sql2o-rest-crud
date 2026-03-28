package com.otis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
	@Value("${redisson.single-server-config.address:redis://localhost:6379}")
	private String address;

	@Value("${redisson.single-server-config.password:}")
	private String password;

	@Value("${redisson.single-server-config.connection-minimum-idle-size:1}")
	private int connectionMinimumIdleSize;

	@Value("${redisson.single-server-config.connection-pool-size:10}")
	private int connectionPoolSize;

	@Bean(destroyMethod = "shutdown")
	public RedissonClient redissonClient() {
		Config config = new Config();
		config.setPassword(password.isEmpty() ? null : password);
		config.useSingleServer()
				.setAddress(address)
				.setConnectionMinimumIdleSize(connectionMinimumIdleSize)
				.setConnectionPoolSize(connectionPoolSize);

		return Redisson.create(config);
	}
}
