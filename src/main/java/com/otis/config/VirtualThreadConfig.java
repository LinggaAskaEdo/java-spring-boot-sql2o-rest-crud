package com.otis.config;

import org.eclipse.jetty.util.thread.VirtualThreadPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jetty.JettyWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VirtualThreadConfig {
	@Value("${spring.threads.virtual.name-prefix:vt-jetty-}")
	private String virtualThreadNamePrefix;

	@Bean
	public WebServerFactoryCustomizer<JettyWebServerFactory> jettyVirtualThreadCustomizer() {
		return factory -> {
			VirtualThreadPool virtualThreadPool = new VirtualThreadPool();
			virtualThreadPool.setName(virtualThreadNamePrefix);

			factory.setThreadPool(virtualThreadPool);
		};
	}
}
