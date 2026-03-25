package com.otis.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sql2o.Sql2o;

@Configuration
public class DatabaseConfig {
	@Bean(name = "sql2oPrimary")
	public Sql2o sql2oPrimary(DataSource datasource) {
		return new Sql2o(datasource);
	}
}
