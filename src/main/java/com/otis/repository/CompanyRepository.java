package com.otis.repository;

import java.util.HashMap;
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
import com.otis.model.Company;
import com.otis.preference.ConstantPreference;

@Repository
public class CompanyRepository {
    private static final Logger logger = LogManager.getLogger();

    private final Sql2o sql2o;
    private final ElSql bundle;

    @Autowired
    public CompanyRepository(Sql2o sql2o) {
        this.sql2o = sql2o;
        this.bundle = ElSql.of(ElSqlConfig.MYSQL, CompanyRepository.class);
    }

    public Company getCompanyByName(String name) {
        String sql = bundle.getSql("GetCompanyByName");
        logger.info("GetCompanyByName: {}", sql);

        Company result = null;
        Map<String, String> colMaps = new HashMap<>();
        colMaps.put(ConstantPreference.COMPANY_ID, ConstantPreference.COMPANYID);

        sql2o.setDefaultColumnMappings(colMaps);

        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sql)) {
            result = query.addParameter("name", name).executeAndFetchFirst(Company.class);
        } catch (Exception e) {
            logger.error("Error when getCompanyByName: ", e);
        }

        return result;
    }
}
