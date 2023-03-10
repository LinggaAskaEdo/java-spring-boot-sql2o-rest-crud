package com.otis.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.Product;
import com.otis.preference.ConstantPreference;

@Repository
public class ProductRepository {
    private static final Logger logger = LogManager.getLogger();

    private final Sql2o sql2o;
    private final ElSql bundle;

    @Autowired
    public ProductRepository(Sql2o sql2o) {
        this.sql2o = sql2o;
        this.bundle = ElSql.of(ElSqlConfig.MYSQL, ProductRepository.class);
    }

    public List<Product> findAll() {
        String sql = bundle.getSql("GetAllProduct");
        logger.info("GetAllProduct: {}", sql);

        List<Product> result = null;
        Map<String, String> colMaps = new HashMap<>();
        colMaps.put(ConstantPreference.COMPANY_ID, ConstantPreference.COMPANYID);

        sql2o.setDefaultColumnMappings(colMaps);

        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sql)) {
            result = query.executeAndFetch(Product.class);
        } catch (Exception e) {
            logger.error("Error when findAll: ", e);
        }

        return result;
    }

    public List<Product> findByNameContaining(String title) {
        String sql = bundle.getSql("GetProductByNameContain");
        logger.info("GetProductByNameContain: {}", sql);

        List<Product> result = null;
        Map<String, String> colMaps = new HashMap<>();
        colMaps.put(ConstantPreference.COMPANY_ID, ConstantPreference.COMPANYID);

        sql2o.setDefaultColumnMappings(colMaps);

        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sql)) {
            result = query.addParameter("name", "%" + title + "%").executeAndFetch(Product.class);
        } catch (Exception e) {
            logger.error("Error when findByNameContaining: ", e);
        }

        return result;
    }

    public List<Product> findProductByCompanyID(long companyID) {
        String sql = bundle.getSql("GetProductByCompanyID");
        logger.info("GetProductByCompanyID: {}", sql);

        List<Product> result = null;
        Map<String, String> colMaps = new HashMap<>();
        colMaps.put("company_id", "companyID");

        sql2o.setDefaultColumnMappings(colMaps);

        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sql)) {
            result = query.addParameter("company_id", companyID).executeAndFetch(Product.class);
        } catch (Exception e) {
            logger.error("Error when findProductByCompanyID: ", e);
        }

        return result;
    }
}
