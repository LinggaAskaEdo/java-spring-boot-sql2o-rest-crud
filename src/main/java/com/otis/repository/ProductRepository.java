package com.otis.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.Product;
import com.otis.preference.ConstantPreference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class ProductRepository {
	private final Sql2o sql2o;
	private final ElSql bundle;

	public ProductRepository(Sql2o sql2o) {
		this.sql2o = sql2o;
		this.bundle = ElSql.of(ElSqlConfig.MYSQL, ProductRepository.class);
	}

	public java.util.List<Product> findByFilters(UUID id, String name, UUID companyId, String companyName) {
		StringBuilder sql = new StringBuilder(bundle.getSql("FindByFilters"));
		String separator = ConstantPreference.WHERE;

		if (id != null) {
			sql.append(separator).append("p.id = :id");
			separator = ConstantPreference.AND;
		}

		if (name != null && !name.isBlank()) {
			sql.append(separator).append("p.name LIKE :name");
			separator = ConstantPreference.AND;
		}

		if (companyId != null) {
			sql.append(separator).append("p.company_id = :companyId");
			separator = ConstantPreference.AND;
		}

		if (companyName != null && !companyName.isBlank()) {
			sql.append(separator).append("c.name LIKE :companyName");
		}

		log.info("FindByFilters: {}", sql);
		try (Connection conn = sql2o.open(); Query query = conn.createQuery(sql.toString())) {
			if (id != null) {
				query.addParameter("id", id.toString());
			}

			if (name != null && !name.isBlank()) {
				query.addParameter("name", "%" + name + "%");
			}

			if (companyId != null) {
				query.addParameter("companyId", companyId.toString());
			}

			if (companyName != null && !companyName.isBlank()) {
				query.addParameter("companyName", "%" + companyName + "%");
			}

			return query.executeAndFetch(Product.class);
		}
	}
}
