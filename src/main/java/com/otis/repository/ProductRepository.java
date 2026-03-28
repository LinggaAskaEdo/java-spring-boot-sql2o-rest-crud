package com.otis.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.entity.PageResponse;
import com.otis.model.entity.Product;
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
		// 1. Build base filter parameters (only non‑null / non‑blank values)
		Map<String, Object> filterParams = new HashMap<>();

		if (id != null) {
			filterParams.put(ConstantPreference.ID, id.toString());
		}

		if (name != null && !name.isBlank()) {
			filterParams.put(ConstantPreference.NAME, "%" + name + "%");
		}

		if (companyId != null) {
			filterParams.put(ConstantPreference.COMPANYID, companyId.toString());
		}

		if (companyName != null && !companyName.isBlank()) {
			filterParams.put(ConstantPreference.COMPANY_NAME, "%" + companyName + "%");
		}

		// 2. Build pagination parameters (for the main query)
		int offset = page * size;
		Map<String, Object> pagingParams = new HashMap<>(filterParams);
		pagingParams.put(ConstantPreference.SIZE, size);
		pagingParams.put(ConstantPreference.OFFSET, offset);

		// 3. Get the dynamic SQL (ElSql will expand @AND and @PAGING)
		String findSql = bundle.getSql("FindByFilters", pagingParams);
		String countSql = bundle.getSql("CountByFilters", filterParams); // count uses only filters

		log.info("FindByFilters: {}", findSql);
		log.info("CountByFilters: {}", countSql);

		try (Connection conn = sql2o.open()) {
			// Main query
			Query query = conn.createQuery(findSql);
			for (Map.Entry<String, Object> entry : pagingParams.entrySet()) {
				query.addParameter(entry.getKey(), entry.getValue());
			}

			var products = query.executeAndFetch(Product.class);

			// Count query
			Query countQuery = conn.createQuery(countSql);
			for (Map.Entry<String, Object> entry : filterParams.entrySet()) {
				countQuery.addParameter(entry.getKey(), entry.getValue());
			}

			long totalElements = countQuery.executeAndFetchFirst(Integer.class);

			int totalPages = (int) Math.ceil((double) totalElements / size);
			boolean isFirst = page == 0;
			boolean isLast = page >= totalPages - 1;

			return new PageResponse<>(products, page, size, totalElements, totalPages, isFirst, isLast);
		}
	}
}