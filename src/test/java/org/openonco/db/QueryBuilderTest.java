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

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link QueryBuilder}.
 * Verifies that SQL queries are generated correctly and safely.
 */
class QueryBuilderTest {

    @Test
    void testBuildSearchQueryBasic() {
        QueryResult result = QueryBuilder.buildSearchQuery(
                "mrd_tests",
                null,  // no filters
                null,  // all fields
                10,
                0
        );

        assertThat(result.sql()).contains("SELECT *");
        assertThat(result.sql()).contains("FROM mrd_tests");
        assertThat(result.sql()).contains("ORDER BY id");
        assertThat(result.sql()).contains("LIMIT 10");
        assertThat(result.params()).isEmpty();
    }

    @Test
    void testBuildSearchQueryWithStringFilter() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("vendor", "Natera");

        QueryResult result = QueryBuilder.buildSearchQuery(
                "mrd_tests",
                filters,
                null,
                50,
                0
        );

        assertThat(result.sql()).contains("WHERE");
        assertThat(result.sql()).contains("vendor ILIKE ?");
        assertThat(result.params()).containsExactly("%Natera%");
    }

    @Test
    void testBuildSearchQueryWithNumericFilter() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("min_sensitivity", 90.0);

        QueryResult result = QueryBuilder.buildSearchQuery(
                "mrd_tests",
                filters,
                null,
                50,
                0
        );

        assertThat(result.sql()).contains("sensitivity >= ?");
        assertThat(result.params()).containsExactly(90.0);
    }

    @Test
    void testBuildSearchQueryWithMaxFilter() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("max_listPrice", 1000.0);

        QueryResult result = QueryBuilder.buildSearchQuery(
                "ecd_tests",
                filters,
                null,
                50,
                0
        );

        assertThat(result.sql()).contains("listPrice <= ?");
        assertThat(result.params()).containsExactly(1000.0);
    }

    @Test
    void testBuildSearchQueryWithArrayFieldFilter() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("cancerTypes", "Colorectal");

        QueryResult result = QueryBuilder.buildSearchQuery(
                "mrd_tests",
                filters,
                null,
                50,
                0
        );

        assertThat(result.sql()).contains("list_contains(cancerTypes, ?)");
        assertThat(result.params()).containsExactly("Colorectal");
    }

    @Test
    void testBuildSearchQueryWithBooleanFilter() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("requiresTumorTissue", true);

        QueryResult result = QueryBuilder.buildSearchQuery(
                "mrd_tests",
                filters,
                null,
                50,
                0
        );

        assertThat(result.sql()).contains("requiresTumorTissue = ?");
        assertThat(result.params()).containsExactly(true);
    }

    @Test
    void testBuildSearchQueryWithMultipleFilters() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("vendor", "Quest");
        filters.put("min_sensitivity", 90.0);

        QueryResult result = QueryBuilder.buildSearchQuery(
                "mrd_tests",
                filters,
                null,
                50,
                0
        );

        assertThat(result.sql()).contains("vendor ILIKE ?");
        assertThat(result.sql()).contains("sensitivity >= ?");
        assertThat(result.sql()).contains(" AND ");
        assertThat(result.params()).containsExactly("%Quest%", 90.0);
    }

    @Test
    void testBuildSearchQueryWithSpecificFields() {
        QueryResult result = QueryBuilder.buildSearchQuery(
                "mrd_tests",
                null,
                List.of("id", "name", "vendor", "sensitivity"),
                50,
                0
        );

        assertThat(result.sql()).contains("SELECT id, name, vendor, sensitivity");
        assertThat(result.sql()).doesNotContain("SELECT *");
    }

    @Test
    void testBuildSearchQueryWithPagination() {
        QueryResult result = QueryBuilder.buildSearchQuery(
                "mrd_tests",
                null,
                null,
                10,
                20
        );

        assertThat(result.sql()).contains("LIMIT 10");
        assertThat(result.sql()).contains("OFFSET 20");
    }

    @Test
    void testBuildSearchQueryEnforcesMaxLimit() {
        QueryResult result = QueryBuilder.buildSearchQuery(
                "mrd_tests",
                null,
                null,
                1000,  // exceeds max
                0
        );

        assertThat(result.sql()).contains("LIMIT 500");  // capped at max
    }

    @Test
    void testBuildGetByIdQuery() {
        QueryResult result = QueryBuilder.buildGetByIdQuery("mrd_tests", "mrd-1");

        assertThat(result.sql()).isEqualTo("SELECT * FROM mrd_tests WHERE id = ?");
        assertThat(result.params()).containsExactly("mrd-1");
    }

    @Test
    void testBuildGetByNameQuery() {
        QueryResult result = QueryBuilder.buildGetByNameQuery("ecd_tests", "Shield");

        assertThat(result.sql()).isEqualTo("SELECT * FROM ecd_tests WHERE name ILIKE ?");
        assertThat(result.params()).containsExactly("Shield");
    }

    @Test
    void testBuildGetByIdsQuery() {
        QueryResult result = QueryBuilder.buildGetByIdsQuery(
                "mrd_tests",
                List.of("mrd-1", "mrd-2", "mrd-3")
        );

        assertThat(result.sql()).isEqualTo("SELECT * FROM mrd_tests WHERE id IN (?, ?, ?)");
        assertThat(result.params()).containsExactly("mrd-1", "mrd-2", "mrd-3");
    }

    @Test
    void testBuildGetByNamesQuery() {
        QueryResult result = QueryBuilder.buildGetByNamesQuery(
                "tds_tests",
                List.of("FoundationOne CDx", "Tempus xT")
        );

        assertThat(result.sql()).contains("SELECT * FROM tds_tests WHERE");
        assertThat(result.sql()).contains("name ILIKE ?");
        assertThat(result.sql()).contains("OR");
        assertThat(result.params()).containsExactly("FoundationOne CDx", "Tempus xT");
    }

    @Test
    void testBuildCountQuery() {
        QueryResult result = QueryBuilder.buildCountQuery("mrd_tests", null);

        assertThat(result.sql()).isEqualTo("SELECT COUNT(*) FROM mrd_tests");
        assertThat(result.params()).isEmpty();
    }

    @Test
    void testBuildCountQueryWithFilters() {
        Map<String, Object> filters = Map.of("vendor", "Natera");

        QueryResult result = QueryBuilder.buildCountQuery("mrd_tests", filters);

        assertThat(result.sql()).contains("SELECT COUNT(*) FROM mrd_tests");
        assertThat(result.sql()).contains("WHERE vendor ILIKE ?");
        assertThat(result.params()).containsExactly("%Natera%");
    }

    @Test
    void testBuildGroupByCountQuery() {
        QueryResult result = QueryBuilder.buildGroupByCountQuery(
                "mrd_tests",
                "vendor",
                null
        );

        assertThat(result.sql()).contains("SELECT vendor, COUNT(*) as count FROM mrd_tests");
        assertThat(result.sql()).contains("GROUP BY vendor");
        assertThat(result.sql()).contains("ORDER BY count DESC");
    }

    @Test
    void testBuildDistinctQuery() {
        QueryResult result = QueryBuilder.buildDistinctQuery("mrd_tests", "vendor");

        assertThat(result.sql()).isEqualTo("SELECT DISTINCT vendor FROM mrd_tests ORDER BY vendor");
        assertThat(result.params()).isEmpty();
    }

    @Test
    void testBuildDistinctQueryForArrayField() {
        QueryResult result = QueryBuilder.buildDistinctQuery("mrd_tests", "cancerTypes");

        assertThat(result.sql()).contains("unnest(cancerTypes)");
        assertThat(result.sql()).contains("as value");
    }

    @Test
    void testInvalidTableNameThrows() {
        assertThatThrownBy(() -> QueryBuilder.buildSearchQuery(
                "invalid_table",
                null,
                null,
                50,
                0
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid table name");
    }

    @Test
    void testInvalidFieldNameThrows() {
        assertThatThrownBy(() -> QueryBuilder.buildSearchQuery(
                "mrd_tests",
                null,
                List.of("valid_field", "1invalid"),  // starts with number
                50,
                0
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid field name");
    }

    @Test
    void testSqlInjectionPrevented() {
        // Attempt SQL injection via table name
        assertThatThrownBy(() -> QueryBuilder.buildSearchQuery(
                "mrd_tests; DROP TABLE users;--",
                null,
                null,
                50,
                0
        )).isInstanceOf(IllegalArgumentException.class);

        // Attempt SQL injection via field name
        assertThatThrownBy(() -> QueryBuilder.buildDistinctQuery(
                "mrd_tests",
                "vendor; DROP TABLE mrd_tests;--"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testEmptyIdsListThrows() {
        assertThatThrownBy(() -> QueryBuilder.buildGetByIdsQuery("mrd_tests", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void testEmptyNamesListThrows() {
        assertThatThrownBy(() -> QueryBuilder.buildGetByNamesQuery("mrd_tests", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void testQueryResultStaticFactories() {
        QueryResult noParams = QueryResult.of("SELECT * FROM mrd_tests");
        assertThat(noParams.sql()).isEqualTo("SELECT * FROM mrd_tests");
        assertThat(noParams.params()).isEmpty();

        QueryResult withParams = QueryResult.of("SELECT * FROM mrd_tests WHERE id = ?", "mrd-1");
        assertThat(withParams.sql()).isEqualTo("SELECT * FROM mrd_tests WHERE id = ?");
        assertThat(withParams.params()).containsExactly("mrd-1");
    }
}
