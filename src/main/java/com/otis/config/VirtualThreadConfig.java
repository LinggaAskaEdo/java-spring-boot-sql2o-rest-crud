package com.otis.config;

import org.eclipse.jetty.util.thread.VirtualThreadPool;
import org.springframework.boot.jetty.JettyWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VirtualThreadConfig {
	@Bean
	public WebServerFactoryCustomizer<JettyWebServerFactory> jettyVirtualThreadCustomizer() {
		return factory -> {
			VirtualThreadPool virtualThreadPool = new VirtualThreadPool();
			virtualThreadPool.setName("vt-jetty-");

			factory.setThreadPool(virtualThreadPool);
		};
	}
}