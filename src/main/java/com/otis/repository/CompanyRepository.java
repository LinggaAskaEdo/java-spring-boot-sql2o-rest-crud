package com.otis.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.entity.Company;
import com.otis.model.entity.PageResponse;
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

	public PageResponse<Company> findByFilters(int page, int size, UUID id, String name) {
		StringBuilder sql = new StringBuilder(bundle.getSql("FindByFilters"));
		StringBuilder countSql = new StringBuilder(bundle.getSql("CountByFilters"));
		String separator = ConstantPreference.WHERE;

		if (id != null) {
			sql.append(separator).append("id = :id");
			countSql.append(separator).append("id = :id");
			separator = ConstantPreference.AND;
		}

		if (name != null && !name.isBlank()) {
			sql.append(separator).append("name LIKE :name");
			countSql.append(separator).append("name LIKE :name");
		}

		int offset = page * size;
		sql.append(" LIMIT :size OFFSET :offset");

		log.info("FindByFilters: {}", sql);
		log.info("CountByFilters: {}", countSql);

		try (Connection conn = sql2o.open()) {
			Query query = conn.createQuery(sql.toString());
			Query countQuery = conn.createQuery(countSql.toString());

			if (id != null) {
				query.addParameter("id", id.toString());
				countQuery.addParameter("id", id.toString());
			}

			if (name != null && !name.isBlank()) {
				query.addParameter("name", "%" + name + "%");
				countQuery.addParameter("name", "%" + name + "%");
			}

			query.addParameter("size", size);
			query.addParameter("offset", offset);

			var companies = query.executeAndFetch(Company.class);
			long totalElements = countQuery.executeAndFetchFirst(Integer.class);

			int totalPages = (int) Math.ceil((double) totalElements / size);
			boolean isFirst = page == 0;
			boolean isLast = page >= totalPages - 1;

			return PageResponse.<Company>builder()
					.content(companies)
					.page(page)
					.size(size)
					.totalElements(totalElements)
					.totalPages(totalPages)
					.first(isFirst)
					.last(isLast)
					.build();
		}
	}
}
