package com.otis.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	public List<Product> findAll() {
		String sql = bundle.getSql("GetAllProduct");
		log.info("GetAllProduct: {}", sql);

		List<Product> result = null;
		Map<String, String> colMaps = new HashMap<>();
		colMaps.put(ConstantPreference.COMPANY_ID, ConstantPreference.COMPANYID);

		sql2o.setDefaultColumnMappings(colMaps);

		try (Connection connection = sql2o.open(); Query query = connection.createQuery(sql)) {
			result = query.executeAndFetch(Product.class);
		} catch (Exception e) {
			log.error("Error when findAll: ", e);
		}

		return result;
	}

	public List<Product> findByNameContaining(String title) {
		String sql = bundle.getSql("GetProductByNameContain");
		log.info("GetProductByNameContain: {}", sql);

		List<Product> result = null;
		Map<String, String> colMaps = new HashMap<>();
		colMaps.put(ConstantPreference.COMPANY_ID, ConstantPreference.COMPANYID);

		sql2o.setDefaultColumnMappings(colMaps);

		try (Connection connection = sql2o.open(); Query query = connection.createQuery(sql)) {
			result = query.addParameter("name", "%" + title + "%").executeAndFetch(Product.class);
		} catch (Exception e) {
			log.error("Error when findByNameContaining: ", e);
		}

		return result;
	}

	public List<Product> findProductByCompanyID(UUID companyID) {
		String sql = bundle.getSql("GetProductByCompanyID");
		log.info("GetProductByCompanyID: {}", sql);

		List<Product> result = null;
		Map<String, String> colMaps = new HashMap<>();
		colMaps.put(ConstantPreference.COMPANY_ID, ConstantPreference.COMPANYID);

		sql2o.setDefaultColumnMappings(colMaps);

		try (Connection connection = sql2o.open(); Query query = connection.createQuery(sql)) {
			result = query.addParameter("company_id", companyID.toString()).executeAndFetch(Product.class);
		} catch (Exception e) {
			log.error("Error when findProductByCompanyID: ", e);
		}

		return result;
	}

	public List<Map<String, Object>> getReportData() {
		String sql = bundle.getSql("GetReportData");
		log.info("GetReportData: {}", sql);

		try (Connection connection = sql2o.open(); Query query = connection.createQuery(sql)) {
			return query.executeAndFetchTable().asList();
		} catch (Exception e) {
			log.error("Error when findProductByCompanyID: ", e);
		}

		return Collections.emptyList();
	}
}
