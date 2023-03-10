package com.otis.repository;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.opengamma.elsql.ElSql;
import com.opengamma.elsql.ElSqlConfig;
import com.otis.model.Tutorial;

@Repository
public class TutorialRepository {
    private static final Logger logger = LogManager.getLogger();

    private final Sql2o sql2o;
    private final ElSql bundle;

    @Autowired
    public TutorialRepository(Sql2o sql2o) {
        this.sql2o = sql2o;
        this.bundle = ElSql.of(ElSqlConfig.MYSQL, TutorialRepository.class);
    }

    public List<Tutorial> findAll() {
        String sql = bundle.getSql("GetAllTutorial");
        logger.info("GetAllTutorial: {}", sql);

        List<Tutorial> result = null;

        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sql)) {
            result = query.executeAndFetch(Tutorial.class);
        } catch (Exception e) {
            logger.error("Error when findAll: ", e);
        }

        return result;
    }

    public List<Tutorial> findByTitleContaining(String title) {
        String sql = bundle.getSql("GetTutorialByTitleContain");
        logger.info("GetTutorialByTitleContain: {}", sql);

        List<Tutorial> result = null;

        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sql)) {
            result = query.addParameter("title", "%" + title + "%").executeAndFetch(Tutorial.class);
        } catch (Exception e) {
            logger.error("Error when findAll: ", e);
        }

        return result;
    }

    public Optional<Tutorial> findById(long id) {
        String sql = bundle.getSql("GetTutorialById");
        logger.info("GetTutorialById: {}", sql);

        Optional<Tutorial> result = Optional.empty();

        try (Connection connection = sql2o.open(); Query query = connection.createQuery(sql)) {
            Tutorial tutorial = query.addParameter("id", id).executeAndFetchFirst(Tutorial.class);

            if (null != tutorial) {
                result = Optional.of(tutorial);
            }
        } catch (Exception e) {
            logger.error("Error when findById: ", e);
        }

        return result;
    }
}
