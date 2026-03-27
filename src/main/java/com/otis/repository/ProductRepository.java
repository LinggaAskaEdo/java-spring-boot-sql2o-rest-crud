package com.otis.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.PageResponse;
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

	public PageResponse<Product> findByFilters(int page, int size, UUID id, String name, UUID companyId,
			String companyName) {
		StringBuilder sql = new StringBuilder(bundle.getSql("FindByFilters"));
		StringBuilder countSql = new StringBuilder(bundle.getSql("CountByFilters"));
		String separator = ConstantPreference.WHERE;

		if (id != null) {
			sql.append(separator).append("p.id = :id");
			countSql.append(separator).append("p.id = :id");
			separator = ConstantPreference.AND;
		}

		if (name != null && !name.isBlank()) {
			sql.append(separator).append("p.name LIKE :name");
			countSql.append(separator).append("p.name LIKE :name");
			separator = ConstantPreference.AND;
		}

		if (companyId != null) {
			sql.append(separator).append("p.company_id = :companyId");
			countSql.append(separator).append("p.company_id = :companyId");
			separator = ConstantPreference.AND;
		}

		if (companyName != null && !companyName.isBlank()) {
			sql.append(separator).append("c.name LIKE :companyName");
			countSql.append(separator).append("c.name LIKE :companyName");
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

			if (companyId != null) {
				query.addParameter("companyId", companyId.toString());
				countQuery.addParameter("companyId", companyId.toString());
			}

			if (companyName != null && !companyName.isBlank()) {
				query.addParameter("companyName", "%" + companyName + "%");
				countQuery.addParameter("companyName", "%" + companyName + "%");
			}

			query.addParameter("size", size);
			query.addParameter("offset", offset);

			var products = query.executeAndFetch(Product.class);
			long totalElements = countQuery.executeAndFetchFirst(Integer.class);

			int totalPages = (int) Math.ceil((double) totalElements / size);
			boolean isFirst = page == 0;
			boolean isLast = page >= totalPages - 1;

			return PageResponse.<Product>builder()
					.content(products)
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
