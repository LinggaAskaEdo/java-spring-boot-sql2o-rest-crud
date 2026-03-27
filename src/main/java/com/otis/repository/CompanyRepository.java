package com.otis.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.Company;
import com.otis.preference.ConstantPreference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class CompanyRepository {
	private final Sql2o sql2o;
	private final ElSql bundle;

	public CompanyRepository(Sql2o sql2o) {
		this.sql2o = sql2o;
		this.bundle = ElSql.of(ElSqlConfig.MYSQL, CompanyRepository.class);
	}

	public java.util.List<Company> findByFilters(UUID id, String name) {
		StringBuilder sql = new StringBuilder(bundle.getSql("FindByFilters"));
		String separator = ConstantPreference.WHERE;

		if (id != null) {
			sql.append(separator).append("id = :id");
			separator = ConstantPreference.AND;
		}

		if (name != null && !name.isBlank()) {
			sql.append(separator).append("name LIKE :name");
		}

		log.info("FindByFilters: {}", sql);
		try (Connection conn = sql2o.open(); Query query = conn.createQuery(sql.toString())) {
			if (id != null) {
				query.addParameter("id", id.toString());
			}

			if (name != null && !name.isBlank()) {
				query.addParameter("name", "%" + name + "%");
			}

			return query.executeAndFetch(Company.class);
		}
	}
}
