package com.otis.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.PageResponse;
import com.otis.model.Tutorial;
import com.otis.preference.ConstantPreference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class TutorialRepository {
	private final Sql2o sql2o;
	private final ElSql bundle;

	public TutorialRepository(Sql2o sql2o) {
		this.sql2o = sql2o;
		this.bundle = ElSql.of(ElSqlConfig.MYSQL, TutorialRepository.class);
	}

	public PageResponse<Tutorial> findByFilters(int page, int size, UUID id, String title, String description,
			Boolean published) {
		StringBuilder sql = new StringBuilder(bundle.getSql("FindByFilters"));
		StringBuilder countSql = new StringBuilder(bundle.getSql("CountByFilters"));
		String separator = ConstantPreference.WHERE;

		if (id != null) {
			sql.append(separator).append("id = :id");
			countSql.append(separator).append("id = :id");
			separator = ConstantPreference.AND;
		}

		if (title != null && !title.isBlank()) {
			sql.append(separator).append("title LIKE :title");
			countSql.append(separator).append("title LIKE :title");
			separator = ConstantPreference.AND;
		}

		if (description != null && !description.isBlank()) {
			sql.append(separator).append("description LIKE :description");
			countSql.append(separator).append("description LIKE :description");
			separator = ConstantPreference.AND;
		}

		if (published != null) {
			sql.append(separator).append("published = :published");
			countSql.append(separator).append("published = :published");
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

			if (title != null && !title.isBlank()) {
				query.addParameter("title", "%" + title + "%");
				countQuery.addParameter("title", "%" + title + "%");
			}

			if (description != null && !description.isBlank()) {
				query.addParameter("description", "%" + description + "%");
				countQuery.addParameter("description", "%" + description + "%");
			}

			if (published != null) {
				query.addParameter("published", published);
				countQuery.addParameter("published", published);
			}

			query.addParameter("size", size);
			query.addParameter("offset", offset);

			var tutorials = query.executeAndFetch(Tutorial.class);
			long totalElements = countQuery.executeAndFetchFirst(Integer.class);

			int totalPages = (int) Math.ceil((double) totalElements / size);
			boolean isFirst = page == 0;
			boolean isLast = page >= totalPages - 1;

			return PageResponse.<Tutorial>builder()
					.content(tutorials)
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
