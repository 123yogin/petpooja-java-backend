package com.example.petpooja_clone.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DatabaseInitializationListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializationListener.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // List of all tables that should exist in the database
    private static final List<String> REQUIRED_TABLES = Arrays.asList(
        "user",
        "menu_item",
        "restaurant_table",
        "orders",
        "order_item",
        "bill",
        "ingredient",
        "menu_ingredient",
        "supplier",
        "purchase_order"
    );

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        // This runs after Hibernate has initialized and created/updated tables
        checkDatabaseTables();
    }

    private void checkDatabaseTables() {
        logger.info("");
        logger.info("========================================");
        logger.info("📊 DATABASE TABLES STATUS CHECK");
        logger.info("========================================");

        try {
            // Check which tables exist after Hibernate initialization
            List<String> existingTables = getExistingTables();
            List<String> missingTables = REQUIRED_TABLES.stream()
                    .filter(table -> !existingTables.contains(table.toLowerCase()))
                    .toList();

            if (existingTables.isEmpty()) {
                logger.warn("⚠️  NO TABLES FOUND in database!");
                logger.info("🔄 Hibernate should have created tables with ddl-auto=update");
                logger.info("   Please check database connection and Hibernate logs above");
            } else {
                logger.info("✅ Found {} existing table(s):", existingTables.size());
                existingTables.forEach(table -> 
                    logger.info("   ✓ Table '{}' exists", table)
                );
            }

            if (!missingTables.isEmpty()) {
                logger.warn("⚠️  Missing {} table(s):", missingTables.size());
                missingTables.forEach(table -> 
                    logger.warn("   ✗ Table '{}' not found", table)
                );
                logger.info("   Hibernate will attempt to create missing tables...");
            } else if (!existingTables.isEmpty()) {
                logger.info("");
                logger.info("✅ SUCCESS: All {} required tables exist!", REQUIRED_TABLES.size());
                logger.info("ℹ️  Hibernate will update schema automatically if needed (ddl-auto=update)");
            }

            logger.info("========================================");
            logger.info("");

        } catch (Exception e) {
            logger.error("❌ Error checking database tables: {}", e.getMessage());
            logger.error("   This might indicate a database connection issue");
            logger.debug("Stack trace:", e);
        }
    }

    private List<String> getExistingTables() {
        try {
            // First, get ALL tables in the public schema to see what actually exists
            String allTablesSql = """
                SELECT tablename 
                FROM pg_tables 
                WHERE schemaname = 'public' 
                ORDER BY tablename
                """;
            
            List<String> allTablesInDb = jdbcTemplate.queryForList(allTablesSql, String.class);
            logger.info("📋 All tables in database: {}", allTablesInDb);
            
            // Now check for our required tables (case-insensitive)
            List<String> existingTables = new java.util.ArrayList<>();
            for (String requiredTable : REQUIRED_TABLES) {
                // Check if table exists (case-insensitive)
                boolean exists = allTablesInDb.stream()
                    .anyMatch(dbTable -> dbTable.equalsIgnoreCase(requiredTable));
                
                if (exists) {
                    // Find the actual case from database
                    String actualTableName = allTablesInDb.stream()
                        .filter(dbTable -> dbTable.equalsIgnoreCase(requiredTable))
                        .findFirst()
                        .orElse(requiredTable);
                    existingTables.add(actualTableName);
                }
            }
            
            return existingTables;
        } catch (Exception e) {
            // Fallback: try information_schema with proper quoting
            logger.warn("Primary table check failed: {}", e.getMessage());
            logger.info("Trying fallback method...");
            return getExistingTablesFallback();
        }
    }
    
    private List<String> getExistingTablesFallback() {
        try {
            // Fallback: check information_schema with case-insensitive matching
            String sql = """
                SELECT LOWER(table_name) as table_name
                FROM information_schema.tables 
                WHERE table_schema = 'public' 
                AND table_type = 'BASE TABLE'
                AND LOWER(table_name) IN (LOWER(?), LOWER(?), LOWER(?), LOWER(?), LOWER(?), 
                                          LOWER(?), LOWER(?), LOWER(?), LOWER(?), LOWER(?))
                ORDER BY table_name
                """;
            
            List<String> allTables = jdbcTemplate.queryForList(sql, String.class,
                "user", "menu_item", "restaurant_table", "orders", 
                "order_item", "bill", "ingredient", "menu_ingredient", 
                "supplier", "purchase_order");
            
            // Normalize to lowercase for comparison
            return allTables.stream()
                    .map(String::toLowerCase)
                    .toList();
        } catch (Exception e) {
            logger.error("Could not query table information: {}", e.getMessage());
            return List.of();
        }
    }
}

