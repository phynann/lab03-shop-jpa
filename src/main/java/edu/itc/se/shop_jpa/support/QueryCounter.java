package edu.itc.se.shop_jpa.support;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

/** Counts JDBC statements prepared by Hibernate since reset(). */
@Component
public class QueryCounter {
    private final Statistics stats;

    public QueryCounter(EntityManagerFactory emf) {
        this.stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
    }

    public void reset() { stats.clear(); }
    public long count() { return stats.getPrepareStatementCount(); }
    public long inserts() { return stats.getEntityInsertCount(); }
}
