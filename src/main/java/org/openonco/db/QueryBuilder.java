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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds parameterized SQL queries from filter criteria.
 * Prevents SQL injection by using prepared statements with parameter binding.
 * <p>
 * Supports DuckDB-specific features like:
 * <ul>
 *   <li>ILIKE for case-insensitive matching</li>
 *   <li>list_contains() for array field searches</li>
 * </ul>
 */
public final class QueryBuilder {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;

    // Fields that are arrays in the JSON data (use list_contains for filtering)
    private static final Set<String> ARRAY_FIELDS = Set.of(
            "cancerTypes",
            "clinicalSettings",
            "biomarkersReported"
    );

    private QueryBuilder() {
        // Utility class, no instantiation
    }

    /**
     * Builds a SELECT query for searching records with optional filters.
     *
     * @param table   The table name (e.g., "mrd_tests")
     * @param filters Map of field names to filter values
     * @param fields  List of fields to return (null for all fields)
     * @param limit   Maximum records to return (default 50, max 500)
     * @param offset  Number of records to skip for pagination
     * @return QueryResult containing SQL and parameter values
     */
    public static QueryResult buildSearchQuery(
            String table,
            Map<String, Object> filters,
            List<String> fields,
            Integer limit,
            Integer offset) {

        validateTableName(table);

        StringBuilder sql = new StringBuilder("SELECT ");

        // Build field list
        if (fields == null || fields.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", fields.stream()
                    .map(QueryBuilder::sanitizeFieldName)
                    .toList()));
        }

        sql.append(" FROM ").append(table);

        // Build WHERE clause
        List<Object> params = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            sql.append(" WHERE ");
            List<String> conditions = new ArrayList<>();

            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String field = entry.getKey();
                Object value = entry.getValue();

                if (value == null) {
                    continue;
                }

                String condition = buildCondition(field, value, params);
                if (condition != null) {
                    conditions.add(condition);
                }
            }

            sql.append(String.join(" AND ", conditions));
        }

        // Add ORDER BY, LIMIT, OFFSET
        sql.append(" ORDER BY id");

        int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        sql.append(" LIMIT ").append(effectiveLimit);

        if (offset != null && offset > 0) {
            sql.append(" OFFSET ").append(offset);
        }

        return new QueryResult(sql.toString(), params.toArray());
    }

    /**
     * Builds a query to get a single record by ID.
     *
     * @param table The table name
     * @param id    The record ID
     * @return QueryResult for fetching by ID
     */
    public static QueryResult buildGetByIdQuery(String table, String id) {
        validateTableName(table);
        return new QueryResult(
                "SELECT * FROM " + table + " WHERE id = ?",
                new Object[]{id}
        );
    }

    /**
     * Builds a query to get a single record by name (case-insensitive).
     *
     * @param table The table name
     * @param name  The record name
     * @return QueryResult for fetching by name
     */
    public static QueryResult buildGetByNameQuery(String table, String name) {
        validateTableName(table);
        return new QueryResult(
                "SELECT * FROM " + table + " WHERE name ILIKE ?",
                new Object[]{name}
        );
    }

    /**
     * Builds a query to get multiple records by IDs.
     *
     * @param table The table name
     * @param ids   List of record IDs
     * @return QueryResult for fetching multiple by IDs
     */
    public static QueryResult buildGetByIdsQuery(String table, List<String> ids) {
        validateTableName(table);
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("IDs list cannot be null or empty");
        }

        String placeholders = String.join(", ", ids.stream().map(id -> "?").toList());
        return new QueryResult(
                "SELECT * FROM " + table + " WHERE id IN (" + placeholders + ")",
                ids.toArray()
        );
    }

    /**
     * Builds a query to get multiple records by names (case-insensitive).
     *
     * @param table The table name
     * @param names List of record names
     * @return QueryResult for fetching multiple by names
     */
    public static QueryResult buildGetByNamesQuery(String table, List<String> names) {
        validateTableName(table);
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("Names list cannot be null or empty");
        }

        List<String> conditions = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            conditions.add("name ILIKE ?");
        }

        return new QueryResult(
                "SELECT * FROM " + table + " WHERE " + String.join(" OR ", conditions),
                names.toArray()
        );
    }

    /**
     * Builds a COUNT query with optional filters.
     *
     * @param table   The table name
     * @param filters Optional filters to apply before counting
     * @return QueryResult for counting records
     */
    public static QueryResult buildCountQuery(String table, Map<String, Object> filters) {
        validateTableName(table);

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(table);

        List<Object> params = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            sql.append(" WHERE ");
            List<String> conditions = new ArrayList<>();

            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String condition = buildCondition(entry.getKey(), entry.getValue(), params);
                if (condition != null) {
                    conditions.add(condition);
                }
            }

            sql.append(String.join(" AND ", conditions));
        }

        return new QueryResult(sql.toString(), params.toArray());
    }

    /**
     * Builds a GROUP BY count query.
     *
     * @param table   The table name
     * @param groupBy The field to group by
     * @param filters Optional filters to apply before grouping
     * @return QueryResult for grouped counting
     */
    public static QueryResult buildGroupByCountQuery(
            String table,
            String groupBy,
            Map<String, Object> filters) {

        validateTableName(table);
        String safeGroupBy = sanitizeFieldName(groupBy);

        StringBuilder sql = new StringBuilder("SELECT ")
                .append(safeGroupBy)
                .append(", COUNT(*) as count FROM ")
                .append(table);

        List<Object> params = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            sql.append(" WHERE ");
            List<String> conditions = new ArrayList<>();

            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String condition = buildCondition(entry.getKey(), entry.getValue(), params);
                if (condition != null) {
                    conditions.add(condition);
                }
            }

            sql.append(String.join(" AND ", conditions));
        }

        sql.append(" GROUP BY ").append(safeGroupBy);
        sql.append(" ORDER BY count DESC");

        return new QueryResult(sql.toString(), params.toArray());
    }

    /**
     * Builds a query to get distinct values for a field.
     *
     * @param table The table name
     * @param field The field to get distinct values for
     * @return QueryResult for distinct values
     */
    public static QueryResult buildDistinctQuery(String table, String field) {
        validateTableName(table);
        String safeField = sanitizeFieldName(field);

        // Handle array fields with UNNEST
        if (ARRAY_FIELDS.contains(field)) {
            return new QueryResult(
                    "SELECT DISTINCT unnest(" + safeField + ") as value FROM " + table + " ORDER BY value",
                    new Object[0]
            );
        }

        return new QueryResult(
                "SELECT DISTINCT " + safeField + " FROM " + table + " ORDER BY " + safeField,
                new Object[0]
        );
    }

    /**
     * Builds a condition string for a filter and adds parameter to the list.
     */
    private static String buildCondition(String field, Object value, List<Object> params) {
        if (value == null) {
            return null;
        }

        // Handle special meta-filters first (these don't map directly to field names)
        switch (field) {
            case "exclude_discontinued" -> {
                // For categories with discontinued tests: exclude by default
                if (Boolean.TRUE.equals(value)) {
                    // No params needed - this is a literal condition
                    return "(isDiscontinued = false OR isDiscontinued IS NULL)";
                }
                return null;  // Don't add filter if false
            }
            case "has_fda_cdx" -> {
                // For TDS: filter by whether test has FDA companion diagnostics
                if (Boolean.TRUE.equals(value)) {
                    return "fdaCompanionDxCount > 0";
                } else if (Boolean.FALSE.equals(value)) {
                    return "(fdaCompanionDxCount = 0 OR fdaCompanionDxCount IS NULL)";
                }
                return null;
            }
        }

        String safeField = sanitizeFieldName(field);

        // Handle standard filter types
        return switch (field) {
            // Numeric comparisons (min_* and max_* prefixes)
            case String f when f.startsWith("min_") -> {
                String actualField = sanitizeFieldName(f.substring(4));
                params.add(value);
                yield actualField + " >= ?";
            }
            case String f when f.startsWith("max_") -> {
                String actualField = sanitizeFieldName(f.substring(4));
                params.add(value);
                yield actualField + " <= ?";
            }

            // Array field contains check
            case String f when ARRAY_FIELDS.contains(f) -> {
                params.add(value.toString());
                yield "list_contains(" + safeField + ", ?)";
            }

            // Boolean fields
            case String f when value instanceof Boolean -> {
                params.add(value);
                yield safeField + " = ?";
            }

            // String fields - use ILIKE for partial, case-insensitive matching
            case String f when value instanceof String -> {
                params.add("%" + value + "%");
                yield safeField + " ILIKE ?";
            }

            // Numeric equality
            case String f when value instanceof Number -> {
                params.add(value);
                yield safeField + " = ?";
            }

            default -> null;
        };
    }

    /**
     * Validates table name to prevent SQL injection.
     * Only allows known table names.
     */
    private static void validateTableName(String table) {
        Set<String> validTables = Set.of("mrd_tests", "ecd_tests", "hct_tests", "tds_tests");
        if (!validTables.contains(table)) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }
    }

    /**
     * Sanitizes a field name to prevent SQL injection.
     * Allows only alphanumeric characters and underscores.
     */
    private static String sanitizeFieldName(String field) {
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be null or empty");
        }
        // Only allow alphanumeric and underscore
        if (!field.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid field name: " + field);
        }
        return field;
    }
}
