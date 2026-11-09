package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class BulkImportService {

    private static final int CHUNK_SIZE = 500;

    private final SessionFactory sessionFactory;
    private final ProductRepository products;

    @PersistenceContext
    private EntityManager entityManager;

    public BulkImportService(
            EntityManagerFactory entityManagerFactory,
            ProductRepository products
    ) {
        this.sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        this.products = products;
    }

    public int importCsv(Path csv) throws IOException {
        int imported = 0;
        try (StatelessSession session = sessionFactory.openStatelessSession();
             BufferedReader reader = Files.newBufferedReader(csv)) {
            Transaction transaction = session.beginTransaction();
            try {
                session.setJdbcBatchSize(CHUNK_SIZE);
                reader.readLine();
                List<Product> chunk = new ArrayList<>(CHUNK_SIZE);
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    chunk.add(parse(line, lineNumber));
                    if (chunk.size() == CHUNK_SIZE) {
                        session.insertMultiple(chunk);
                        imported += chunk.size();
                        chunk.clear();
                    }
                }
                if (!chunk.isEmpty()) {
                    session.insertMultiple(chunk);
                    imported += chunk.size();
                }
                transaction.commit();
                return imported;
            } catch (IOException | RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    @Transactional(rollbackFor = IOException.class)
    public int importWithSaveAll(Path csv) throws IOException {
        return importWithSaveAll(csv, false);
    }

    @Transactional(rollbackFor = IOException.class)
    public int importWithSaveAllAndClear(Path csv) throws IOException {
        return importWithSaveAll(csv, true);
    }

    private int importWithSaveAll(Path csv, boolean clearPersistenceContext)
            throws IOException {
        int imported = 0;
        try (BufferedReader reader = Files.newBufferedReader(csv)) {
            reader.readLine();
            List<Product> chunk = new ArrayList<>(CHUNK_SIZE);
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                chunk.add(parse(line, lineNumber));
                if (chunk.size() == CHUNK_SIZE) {
                    products.saveAll(chunk);
                    imported += chunk.size();
                    chunk.clear();
                    if (clearPersistenceContext) {
                        products.flush();
                        entityManager.clear();
                    }
                }
            }
            if (!chunk.isEmpty()) {
                products.saveAll(chunk);
                imported += chunk.size();
                if (clearPersistenceContext) {
                    products.flush();
                    entityManager.clear();
                }
            }
        }
        return imported;
    }

    private Product parse(String line, int lineNumber) {
        String[] fields = line.split(";", -1);
        if (fields.length != 6) {
            throw new IllegalArgumentException(
                    "Invalid product CSV at line " + lineNumber
                            + ": expected 6 semicolon-separated fields"
            );
        }
        try {
            Product product = new Product(
                    fields[0].trim(),
                    fields[1].trim(),
                    new Money(new BigDecimal(fields[2].trim()), fields[3].trim()),
                    Category.valueOf(fields[4].trim())
            );
            int stock = Integer.parseInt(fields[5].trim());
            if (stock < 0) {
                throw new IllegalArgumentException("stock must not be negative");
            }
            product.setStock(stock);
            return product;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid product CSV at line " + lineNumber,
                    exception
            );
        }
    }
}
