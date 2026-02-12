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

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DuckDbService}.
 * Verifies that JSON data files are correctly loaded into DuckDB tables
 * and that basic queries execute successfully.
 */
@QuarkusTest
class DuckDbServiceTest {

    @Inject
    DuckDbService service;

    @Test
    void testConnectionIsValid() {
        assertThat(service.isConnected()).isTrue();
    }

    @Test
    void testMrdTableLoaded() {
        int count = service.getTableRowCount("mrd_tests");
        assertThat(count).isGreaterThan(0);
        System.out.println("MRD tests count: " + count);
    }

    @Test
    void testEcdTableLoaded() {
        int count = service.getTableRowCount("ecd_tests");
        assertThat(count).isGreaterThan(0);
        System.out.println("ECD tests count: " + count);
    }

    @Test
    void testHctTableLoaded() {
        int count = service.getTableRowCount("hct_tests");
        assertThat(count).isGreaterThan(0);
        System.out.println("HCT tests count: " + count);
    }

    @Test
    void testTdsTableLoaded() {
        int count = service.getTableRowCount("tds_tests");
        assertThat(count).isGreaterThan(0);
        System.out.println("TDS tests count: " + count);
    }

    @Test
    void testPapTableLoaded() {
        int count = service.getTableRowCount("pap_programs");
        assertThat(count).isGreaterThan(0);
        System.out.println("PAP programs count: " + count);
    }

    @Test
    void testQueryByIdReturnsRecord() {
        List<Map<String, Object>> results = service.executeQuery(
                "SELECT * FROM mrd_tests WHERE id = ?", "mrd-1"
        );

        assertThat(results).hasSize(1);
        Map<String, Object> record = results.get(0);
        assertThat(record.get("id")).isEqualTo("mrd-1");
        assertThat(record.get("name")).isNotNull();
        assertThat(record.get("vendor")).isNotNull();
    }

    @Test
    void testQueryByNameReturnsRecord() {
        // First, get a known name from the database
        List<Map<String, Object>> allRecords = service.executeQuery(
                "SELECT name FROM mrd_tests LIMIT 1"
        );
        assertThat(allRecords).isNotEmpty();

        String knownName = (String) allRecords.get(0).get("name");

        // Now query by that name
        List<Map<String, Object>> results = service.executeQuery(
                "SELECT * FROM mrd_tests WHERE name ILIKE ?", knownName
        );

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).get("name")).isEqualTo(knownName);
    }

    @Test
    void testArrayFieldQuery() {
        // Test querying array fields with list_contains
        List<Map<String, Object>> results = service.executeQuery(
                "SELECT id, name, cancerTypes FROM mrd_tests WHERE list_contains(cancerTypes, ?) LIMIT 5",
                "Colorectal"
        );

        // May or may not have results depending on data, but query should not fail
        assertThat(results).isNotNull();

        if (!results.isEmpty()) {
            // Verify cancerTypes is returned as a list
            Object cancerTypes = results.get(0).get("cancerTypes");
            assertThat(cancerTypes).isInstanceOf(List.class);
        }
    }

    @Test
    void testCountQuery() {
        int count = service.executeCount("SELECT COUNT(*) FROM mrd_tests WHERE vendor ILIKE ?", "%Quest%");
        // Count could be 0 or more, but should not throw
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testQueryWithMultipleParams() {
        List<Map<String, Object>> results = service.executeQuery(
                "SELECT id, name, vendor, sensitivity FROM mrd_tests WHERE sensitivity >= ? ORDER BY sensitivity DESC LIMIT ?",
                90.0, 5
        );

        assertThat(results).isNotNull();
        // All returned records should have sensitivity >= 90
        for (Map<String, Object> row : results) {
            Object sensitivity = row.get("sensitivity");
            if (sensitivity instanceof Number) {
                assertThat(((Number) sensitivity).doubleValue()).isGreaterThanOrEqualTo(90.0);
            }
        }
    }

    @Test
    void testQueryResultOverload() {
        QueryResult query = QueryResult.of(
                "SELECT id, name FROM mrd_tests WHERE id = ?",
                "mrd-1"
        );

        List<Map<String, Object>> results = service.executeQuery(query);
        assertThat(results).hasSize(1);
    }

    @Test
    void testDistinctVendors() {
        List<Map<String, Object>> results = service.executeQuery(
                "SELECT DISTINCT vendor FROM mrd_tests ORDER BY vendor"
        );

        assertThat(results).isNotEmpty();
        // Verify all vendors are unique
        List<String> vendors = results.stream()
                .map(row -> (String) row.get("vendor"))
                .toList();
        assertThat(vendors).doesNotHaveDuplicates();
    }

    @Test
    void testUnnestArrayField() {
        // Test DuckDB's unnest for array fields
        List<Map<String, Object>> results = service.executeQuery(
                "SELECT DISTINCT unnest(cancerTypes) as cancer_type FROM mrd_tests ORDER BY cancer_type LIMIT 10"
        );

        assertThat(results).isNotEmpty();
        for (Map<String, Object> row : results) {
            assertThat(row.get("cancer_type")).isNotNull();
        }
    }

    @Test
    void testRequiredFieldsNotNull() {
        // Verify no records have null required fields (validation should have caught this at startup)
        for (String table : List.of("mrd_tests", "ecd_tests", "hct_tests", "tds_tests")) {
            int nullCount = service.executeCount(
                    "SELECT COUNT(*) FROM " + table + " WHERE id IS NULL OR name IS NULL OR vendor IS NULL"
            );
            assertThat(nullCount)
                    .as("Table %s should have no null required fields", table)
                    .isEqualTo(0);
        }
    }

    @Test
    void testAllTablesHaveData() {
        // Summary test to verify all tables loaded
        int mrdCount = service.getTableRowCount("mrd_tests");
        int ecdCount = service.getTableRowCount("ecd_tests");
        int hctCount = service.getTableRowCount("hct_tests");
        int tdsCount = service.getTableRowCount("tds_tests");
        int papCount = service.getTableRowCount("pap_programs");

        System.out.println("=== OpenOnco Database Summary ===");
        System.out.println("MRD tests: " + mrdCount);
        System.out.println("ECD tests: " + ecdCount);
        System.out.println("HCT tests: " + hctCount);
        System.out.println("TDS tests: " + tdsCount);
        System.out.println("PAP programs: " + papCount);
        System.out.println("Total: " + (mrdCount + ecdCount + hctCount + tdsCount + papCount));

        assertThat(mrdCount).as("MRD table").isGreaterThan(0);
        assertThat(ecdCount).as("ECD table").isGreaterThan(0);
        assertThat(hctCount).as("HCT table").isGreaterThan(0);
        assertThat(tdsCount).as("TDS table").isGreaterThan(0);
        assertThat(papCount).as("PAP table").isGreaterThan(0);
    }
}
