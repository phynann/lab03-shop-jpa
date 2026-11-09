package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.support.QueryCounter;
import edu.itc.se.shop_jpa.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
class NPlusOneTest {
    @Autowired ReportService report;
    @Autowired QueryCounter counter;
    @Autowired TestData data;

    @BeforeEach
    void seed() { data.paidOrders(50, 3); }

    @Test
    @Tag("demo")
    void naiveImplementationRunsNPlusOneQueries() {
        counter.reset();
        report.linesPerPaidOrder();
        assertThat(counter.count()).isEqualTo(51);
    }

    @Test
    void joinFetchRunsOneQuery() {
        counter.reset();
        report.linesPerPaidOrderJoinFetch();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void entityGraphRunsOneQuery() {
        counter.reset();
        report.linesPerPaidOrderEntityGraph();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void batchSizeRunsOnePlusCeilNOverBatch() {
        counter.reset();
        report.linesPerPaidOrderBatched();
        assertThat(counter.count()).isEqualTo(1 + (int) Math.ceil(50 / 16.0));
    }
}
