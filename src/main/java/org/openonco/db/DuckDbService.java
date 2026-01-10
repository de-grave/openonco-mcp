/*
 * Copyright © 2026 OpenOnco
 * Copyright © 2026 Dmitry Degrave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openonco.db;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing the in-memory DuckDB database.
 * <p>
 * Initializes at startup by:
 * <ol>
 *   <li>Creating an in-memory DuckDB connection</li>
 *   <li>Extracting JSON files from classpath to temporary files</li>
 *   <li>Loading JSON data into DuckDB tables using read_json_auto()</li>
 *   <li>Validating required fields exist in all records</li>
 * </ol>
 * <p>
 * Provides methods to execute parameterized queries safely.
 */
@ApplicationScoped
@Startup
public class DuckDbService {

    private static final String[] REQUIRED_FIELDS = {"id", "name", "vendor"};

    private static final Map<String, String> TABLE_RESOURCES = Map.of(
            "mrd_tests", "/mrd.json",
            "ecd_tests", "/ecd.json",
            "trm_tests", "/trm.json",
            "tds_tests", "/tds.json"
    );

    private Connection connection;
    private final List<Path> tempFiles = new ArrayList<>();

    @PostConstruct
    void init() {
        Log.info("Initializing DuckDB in-memory database...");

        try {
            // Initialize in-memory DuckDB connection
            connection = DriverManager.getConnection("jdbc:duckdb:");
            Log.info("DuckDB connection established");

            // Load each JSON file into its table
            for (Map.Entry<String, String> entry : TABLE_RESOURCES.entrySet()) {
                String tableName = entry.getKey();
                String resourcePath = entry.getValue();
                loadJsonTable(tableName, resourcePath);
            }

            Log.info("DuckDB initialization complete");

        } catch (SQLException e) {
            throw new OpenOncoException(
                    OpenOncoException.ErrorCode.INITIALIZATION_ERROR,
                    "Failed to initialize DuckDB: " + e.getMessage(),
                    e
            );
        }
    }

    @PreDestroy
    void cleanup() {
        Log.info("Cleaning up DuckDB resources...");

        // Close connection
        if (connection != null) {
            try {
                connection.close();
                Log.info("DuckDB connection closed");
            } catch (SQLException e) {
                Log.warn("Error closing DuckDB connection: " + e.getMessage());
            }
        }

        // Delete temp files
        for (Path tempFile : tempFiles) {
            try {
                Files.deleteIfExists(tempFile);
                Log.debug("Deleted temp file: " + tempFile);
            } catch (IOException e) {
                Log.warn("Failed to delete temp file: " + tempFile + " - " + e.getMessage());
            }
        }
    }

    /**
     * Executes a parameterized query and returns results as a list of maps.
     *
     * @param sql    The SQL query with ? placeholders
     * @param params The parameter values to bind
     * @return List of maps, each representing a row with column names as keys
     */
    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                return resultSetToList(rs);
            }
        } catch (SQLException e) {
            throw new OpenOncoException(
                    OpenOncoException.ErrorCode.QUERY_ERROR,
                    "Query execution failed: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Executes a parameterized query using a QueryResult.
     *
     * @param queryResult The QueryResult containing SQL and parameters
     * @return List of maps representing the result rows
     */
    public List<Map<String, Object>> executeQuery(QueryResult queryResult) {
        return executeQuery(queryResult.sql(), queryResult.params());
    }

    /**
     * Executes a COUNT query and returns the count.
     *
     * @param sql    The COUNT SQL query
     * @param params The parameter values to bind
     * @return The count result
     */
    public int executeCount(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            bindParameters(stmt, params);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new OpenOncoException(
                    OpenOncoException.ErrorCode.QUERY_ERROR,
                    "Count query failed: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Executes a COUNT query using a QueryResult.
     *
     * @param queryResult The QueryResult containing SQL and parameters
     * @return The count result
     */
    public int executeCount(QueryResult queryResult) {
        return executeCount(queryResult.sql(), queryResult.params());
    }

    /**
     * Gets the row count for a table.
     *
     * @param tableName The table name
     * @return Number of rows in the table
     */
    public int getTableRowCount(String tableName) {
        return executeCount("SELECT COUNT(*) FROM " + tableName);
    }

    /**
     * Checks if the database connection is valid.
     *
     * @return true if connected and valid
     */
    public boolean isConnected() {
        try {
            return connection != null && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Loads a JSON file from classpath into a DuckDB table.
     */
    private void loadJsonTable(String tableName, String resourcePath) {
        Log.infof("Loading %s from %s...", tableName, resourcePath);

        try {
            // Extract resource to temp file
            Path tempFile = extractResourceToTempFile(resourcePath);
            tempFiles.add(tempFile);

            // Create table from JSON using DuckDB's read_json_auto
            String createTableSql = String.format(
                    "CREATE TABLE %s AS SELECT * FROM read_json_auto('%s')",
                    tableName,
                    tempFile.toAbsolutePath().toString().replace("\\", "/")
            );

            try (PreparedStatement stmt = connection.prepareStatement(createTableSql)) {
                stmt.execute();
            }

            // Validate required fields
            validateRequiredFields(tableName);

            // Log row count
            int rowCount = getTableRowCount(tableName);
            Log.infof("Loaded %d records into %s", rowCount, tableName);

        } catch (IOException e) {
            throw new OpenOncoException(
                    OpenOncoException.ErrorCode.INITIALIZATION_ERROR,
                    "Failed to extract resource " + resourcePath + ": " + e.getMessage(),
                    e
            );
        } catch (SQLException e) {
            throw new OpenOncoException(
                    OpenOncoException.ErrorCode.INITIALIZATION_ERROR,
                    "Failed to load table " + tableName + ": " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Extracts a classpath resource to a temporary file.
     */
    private Path extractResourceToTempFile(String resourcePath) throws IOException {
        // Determine file prefix from resource name (e.g., "/mrd.json" -> "mrd")
        String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        String prefix = fileName.replace(".json", "");

        Path tempFile = Files.createTempFile("openonco-" + prefix + "-", ".json");

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        Log.debugf("Extracted %s to %s", resourcePath, tempFile);
        return tempFile;
    }

    /**
     * Validates that all records have non-null required fields.
     */
    private void validateRequiredFields(String tableName) throws SQLException {
        // Build condition for null/empty required fields
        StringBuilder condition = new StringBuilder();
        for (int i = 0; i < REQUIRED_FIELDS.length; i++) {
            if (i > 0) {
                condition.append(" OR ");
            }
            condition.append(REQUIRED_FIELDS[i]).append(" IS NULL");
        }

        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + condition;

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next() && rs.getInt(1) > 0) {
                int invalidCount = rs.getInt(1);

                // Get details about invalid records
                String detailsSql = "SELECT id, name, vendor FROM " + tableName +
                        " WHERE " + condition + " LIMIT 5";

                try (PreparedStatement detailStmt = connection.prepareStatement(detailsSql);
                     ResultSet detailRs = detailStmt.executeQuery()) {

                    StringBuilder details = new StringBuilder();
                    while (detailRs.next()) {
                        details.append("\n  - id=").append(detailRs.getString("id"))
                                .append(", name=").append(detailRs.getString("name"))
                                .append(", vendor=").append(detailRs.getString("vendor"));
                    }

                    throw new OpenOncoException(
                            OpenOncoException.ErrorCode.DATA_VALIDATION_ERROR,
                            String.format(
                                    "Table %s has %d record(s) with missing required fields (id, name, or vendor).%s",
                                    tableName, invalidCount, details
                            ),
                            "Check the source JSON file for incomplete records"
                    );
                }
            }
        }

        Log.debugf("Validated required fields for %s", tableName);
    }

    /**
     * Binds parameters to a prepared statement.
     */
    private void bindParameters(PreparedStatement stmt, Object[] params) throws SQLException {
        if (params == null) {
            return;
        }

        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            int paramIndex = i + 1;

            if (param == null) {
                stmt.setNull(paramIndex, java.sql.Types.NULL);
            } else if (param instanceof String) {
                stmt.setString(paramIndex, (String) param);
            } else if (param instanceof Integer) {
                stmt.setInt(paramIndex, (Integer) param);
            } else if (param instanceof Long) {
                stmt.setLong(paramIndex, (Long) param);
            } else if (param instanceof Double) {
                stmt.setDouble(paramIndex, (Double) param);
            } else if (param instanceof Float) {
                stmt.setFloat(paramIndex, (Float) param);
            } else if (param instanceof Boolean) {
                stmt.setBoolean(paramIndex, (Boolean) param);
            } else {
                stmt.setObject(paramIndex, param);
            }
        }
    }

    /**
     * Converts a ResultSet to a list of maps.
     */
    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                Object value = rs.getObject(i);

                // Convert DuckDB arrays to Java lists
                if (value instanceof Array) {
                    Array array = (Array) value;
                    Object[] arrayData = (Object[]) array.getArray();
                    value = List.of(arrayData);
                }

                row.put(columnName, value);
            }
            results.add(row);
        }

        return results;
    }
}
