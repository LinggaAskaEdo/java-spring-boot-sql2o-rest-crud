package com.otis.repository;

import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
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

	public java.util.List<Tutorial> findByFilters(UUID id, String title, String description, Boolean published) {
		StringBuilder sql = new StringBuilder(bundle.getSql("FindByFilters"));
		String separator = ConstantPreference.WHERE;

		if (id != null) {
			sql.append(separator).append("id = :id");
			separator = ConstantPreference.AND;
		}

		if (title != null && !title.isBlank()) {
			sql.append(separator).append("title LIKE :title");
			separator = ConstantPreference.AND;
		}

		if (description != null && !description.isBlank()) {
			sql.append(separator).append("description LIKE :description");
			separator = ConstantPreference.AND;
		}

		if (published != null) {
			sql.append(separator).append("published = :published");
		}

		log.info("FindByFilters: {}", sql);
		try (Connection conn = sql2o.open(); Query query = conn.createQuery(sql.toString())) {
			if (id != null) {
				query.addParameter("id", id.toString());
			}

			if (title != null && !title.isBlank()) {
				query.addParameter("title", "%" + title + "%");
			}

			if (description != null && !description.isBlank()) {
				query.addParameter("description", "%" + description + "%");
			}

			if (published != null) {
				query.addParameter("published", published);
			}

			return query.executeAndFetch(Tutorial.class);
		}
	}
}
