package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class BulkImportTest {

    private static final Logger log =
            LoggerFactory.getLogger(BulkImportTest.class);
    private static final int PRODUCT_COUNT = 50_000;

    @Autowired
    private BulkImportService importer;

    @Autowired
    private ProductRepository products;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void comparesThreeFiftyThousandRowImportStrategies() throws Exception {
        Path csv = Files.createTempFile("shop-products-", ".csv");
        String prefix = "BI-" + UUID.randomUUID().toString().substring(0, 8) + "-";
        try {
            writeCsv(csv, prefix);
            measure("Stateless insertMultiple(500)", csv, prefix, importer::importCsv);
            measure("saveAll, one tx, no clear", csv, prefix, importer::importWithSaveAll);
            measure("saveAll + flush/clear(500)", csv, prefix,
                    importer::importWithSaveAllAndClear);
        } finally {
            Files.deleteIfExists(csv);
        }
    }

    private void measure(
            String strategy,
            Path csv,
            String prefix,
            ImportOperation operation
    ) throws Exception {
        long baselineHeap = usedHeap();
        AtomicLong peakHeap = new AtomicLong(baselineHeap);
        Thread sampler = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                peakHeap.accumulateAndGet(usedHeap(), Math::max);
                try {
                    Thread.sleep(5);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "heap-sampler");
        sampler.setDaemon(true);
        sampler.start();
        long startedAt = System.nanoTime();
        try {
            assertEquals(PRODUCT_COUNT, operation.run(csv));
        } finally {
            sampler.interrupt();
            sampler.join();
        }
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        peakHeap.accumulateAndGet(usedHeap(), Math::max);
        Long actualCount = jdbc.queryForObject(
                "select count(*) from product where sku like ?",
                Long.class,
                prefix + "%"
        );
        assertEquals(PRODUCT_COUNT, actualCount);
        log.info("{}: {} rows in {} ms, sampled peak heap +{} MB",
                strategy, PRODUCT_COUNT, elapsedMillis,
                (peakHeap.get() - baselineHeap) / (1024 * 1024));
    }

    private void writeCsv(Path csv, String prefix) throws Exception {
        try (BufferedWriter writer = Files.newBufferedWriter(csv)) {
            writer.write("sku;name;amount;currency;category;stock");
            writer.newLine();
            for (int i = 0; i < PRODUCT_COUNT; i++) {
                writer.write(prefix + i + ";Imported product " + i
                        + ";9.99;EUR;HARDWARE;10");
                writer.newLine();
            }
        }
    }

    private long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    @FunctionalInterface
    private interface ImportOperation {
        int run(Path csv) throws Exception;
    }
}
