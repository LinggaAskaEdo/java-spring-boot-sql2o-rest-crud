package com.otis.scheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.util.RandomUtils;
import com.otis.util.UuidUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DataSeederScheduler {
	private final Sql2o sql2o;
	private final ElSql bundle;
	private final boolean enabled;
	private final int totalCompanies;
	private final int totalProducts;
	private final int totalTutorials;
	private final int maxCompaniesPerProduct;

	public DataSeederScheduler(Sql2o sql2o,
			@Value("${scheduler.data-seeder.enabled:false}") boolean enabled,
			@Value("${scheduler.data-seeder.total-companies:10}") int totalCompanies,
			@Value("${scheduler.data-seeder.total-products:15}") int totalProducts,
			@Value("${scheduler.data-seeder.total-tutorials:10}") int totalTutorials,
			@Value("${scheduler.data-seeder.max-companies-per-product:3}") int maxCompaniesPerProduct) {
		this.sql2o = sql2o;
		this.bundle = ElSql.of(ElSqlConfig.MYSQL, DataSeederScheduler.class);
		this.enabled = enabled;
		this.totalCompanies = totalCompanies;
		this.totalProducts = totalProducts;
		this.totalTutorials = totalTutorials;
		this.maxCompaniesPerProduct = maxCompaniesPerProduct;
	}

	@Scheduled(cron = "${scheduler.data-seeder.cron:0 0 0 * * ?}")
	public void seedData() {
		if (!enabled) {
			log.debug("Data seeder is disabled");
			return;
		}

		log.info("Starting data seeding with {} companies, {} products, {} tutorials...",
				totalCompanies, totalProducts, totalTutorials);
		seedCompanies();
		seedProducts();
		seedProductsCompany();
		seedTutorials();
		log.info("Data seeding completed");
	}

	private void seedCompanies() {
		if (!isTableEmpty("company")) {
			log.info("Companies already exist, skipping seed");
			return;
		}

		try (Connection connection = sql2o.open()) {
			for (int i = 0; i < totalCompanies; i++) {
				String sql = bundle.getSql("InsertCompany");
				try (Query query = connection.createQuery(sql)) {
					query.addParameter("id", UuidUtils.randomUuidV7().toString())
							.addParameter("name", RandomUtils.randomCompanyName())
							.executeUpdate();
				}
			}

			log.info("Seeded {} companies", totalCompanies);
		}
	}

	private void seedProducts() {
		if (!isTableEmpty("products")) {
			log.info("Products already exist, skipping seed");
			return;
		}

		List<UUID> companyIds = getAllCompanyIds();
		if (companyIds.isEmpty()) {
			log.warn("No companies found, skipping product seeding");
			return;
		}

		try (Connection connection = sql2o.open()) {
			for (int i = 0; i < totalProducts; i++) {
				UUID randomCompanyId = companyIds.get(RandomUtils.randomInt(companyIds.size()));
				String sql = bundle.getSql("InsertProduct");
				try (Query query = connection.createQuery(sql)) {
					query.addParameter("id", UuidUtils.randomUuidV7().toString())
							.addParameter("name", RandomUtils.randomProductName())
							.addParameter("companyId", randomCompanyId.toString())
							.executeUpdate();
				}
			}

			log.info("Seeded {} products", totalProducts);
		}
	}

	private void seedProductsCompany() {
		if (!isTableEmpty("products_company")) {
			log.info("Products-Company relations already exist, skipping seed");
			return;
		}

		List<UUID> companyIds = getAllCompanyIds();
		List<UUID> productIds = getAllProductIds();

		if (companyIds.isEmpty() || productIds.isEmpty()) {
			log.warn("No companies or products found, skipping junction table seeding");
			return;
		}

		Set<String> insertedRelations = new HashSet<>();
		try (Connection connection = sql2o.open()) {
			for (UUID productId : productIds) {
				int numCompanies = RandomUtils.randomInt(maxCompaniesPerProduct) + 1;
				Set<UUID> selectedCompanies = new HashSet<>();

				for (int i = 0; i < numCompanies && selectedCompanies.size() < companyIds.size(); i++) {
					UUID randomCompanyId = companyIds.get(RandomUtils.randomInt(companyIds.size()));
					if (selectedCompanies.add(randomCompanyId)) {
						String relationKey = productId + "_" + randomCompanyId;
						if (!insertedRelations.contains(relationKey)) {
							insertedRelations.add(relationKey);
							String sql = bundle.getSql("InsertProductsCompany");
							try (Query query = connection.createQuery(sql)) {
								query.addParameter("productId", productId.toString())
										.addParameter("companyId", randomCompanyId.toString())
										.executeUpdate();
							}
						}
					}
				}
			}

			log.info("Seeded {} product-company relations", insertedRelations.size());
		}
	}

	private void seedTutorials() {
		if (!isTableEmpty("tutorials")) {
			log.info("Tutorials already exist, skipping seed");
			return;
		}

		try (Connection connection = sql2o.open()) {
			for (int i = 0; i < totalTutorials; i++) {
				String sql = bundle.getSql("InsertTutorial");
				try (Query query = connection.createQuery(sql)) {
					query.addParameter("id", UuidUtils.randomUuidV7().toString())
							.addParameter("title", RandomUtils.randomTutorialTitle())
							.addParameter("description", RandomUtils.randomTutorialDescription())
							.addParameter("published", RandomUtils.randomBoolean())
							.executeUpdate();
				}
			}

			log.info("Seeded {} tutorials", totalTutorials);
		}
	}

	private boolean isTableEmpty(String tableName) {
		String sql = "SELECT COUNT(*) AS cnt FROM " + tableName;
		try (Connection connection = sql2o.open();
				Query query = connection.createQuery(sql)) {
			List<Map<String, Object>> result = query.executeAndFetchTable().asList();
			return result.get(0).get("cnt").equals(0L);
		}
	}

	private List<UUID> getAllCompanyIds() {
		List<UUID> ids = new ArrayList<>();
		String sql = "SELECT id FROM company";

		try (Connection connection = sql2o.open();
				Query query = connection.createQuery(sql)) {
			List<Map<String, Object>> result = query.executeAndFetchTable().asList();
			for (Map<String, Object> row : result) {
				ids.add(UUID.fromString(row.get("id").toString()));
			}
		}
		return ids;
	}

	private List<UUID> getAllProductIds() {
		List<UUID> ids = new ArrayList<>();
		String sql = "SELECT id FROM products";
		try (Connection connection = sql2o.open();
				Query query = connection.createQuery(sql)) {
			List<Map<String, Object>> result = query.executeAndFetchTable().asList();
			for (Map<String, Object> row : result) {
				ids.add(UUID.fromString(row.get("id").toString()));
			}
		}

		return ids;
	}
}
