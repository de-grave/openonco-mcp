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

package org.openonco.mcp;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.openonco.client.OpenOncoClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the OpenOnco MCP get and compare tools.
 * Tests all 8 detail/compare tools.
 */
@QuarkusTest
class DetailCompareToolsTest {

    @Inject
    OpenOncoClient client;

    // ========================================
    // GET MRD TESTS
    // ========================================

    @Test
    void testGetMrd_ById() {
        String result = client.getMrd("mrd-1", null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("{");  // Single object, not array
        assertThat(result).contains("\"id\": \"mrd-1\"");
        assertThat(result).contains("\"name\":");
        assertThat(result).contains("\"vendor\":");
        System.out.println("getMrd by id: success");
    }

    @Test
    void testGetMrd_ByName() {
        // First get a known name
        String searchResult = client.searchMrd(null, null, null, null, null, null, null, "name", 1, null);
        // Extract first name from search results
        String knownName = extractFirstValue(searchResult, "name");

        if (knownName != null) {
            String result = client.getMrd(null, knownName);

            assertThat(result).isNotNull();
            assertThat(result).startsWith("{");
            assertThat(result).contains("\"name\": \"" + knownName + "\"");
            System.out.println("getMrd by name '" + knownName + "': success");
        }
    }

    @Test
    void testGetMrd_NotFound() {
        String result = client.getMrd("mrd-nonexistent", null);

        assertThat(result).contains("\"error\": true");
        assertThat(result).contains("\"code\": \"NOT_FOUND\"");
        assertThat(result).contains("\"suggestion\":");
        System.out.println("getMrd not found: error response correct");
    }

    @Test
    void testGetMrd_MissingParameter() {
        String result = client.getMrd(null, null);

        assertThat(result).contains("\"error\": true");
        assertThat(result).contains("\"code\": \"MISSING_PARAMETER\"");
        assertThat(result).contains("Either 'id' or 'name' must be provided");
        System.out.println("getMrd missing parameter: error response correct");
    }

    @Test
    void testGetMrd_IdTakesPrecedence() {
        // If both id and name provided, id should be used
        String result = client.getMrd("mrd-1", "SomeDifferentName");

        assertThat(result).isNotNull();
        assertThat(result).startsWith("{");
        assertThat(result).contains("\"id\": \"mrd-1\"");
        System.out.println("getMrd id precedence: correct");
    }

    // ========================================
    // GET ECD TESTS
    // ========================================

    @Test
    void testGetEcd_ById() {
        String result = client.getEcd("ecd-1", null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("{");
        assertThat(result).contains("\"id\": \"ecd-1\"");
        System.out.println("getEcd by id: success");
    }

    @Test
    void testGetEcd_NotFound() {
        String result = client.getEcd("ecd-nonexistent", null);

        assertThat(result).contains("\"error\": true");
        assertThat(result).contains("\"code\": \"NOT_FOUND\"");
        System.out.println("getEcd not found: error response correct");
    }

    // ========================================
    // GET HCT TESTS
    // ========================================

    @Test
    void testGetHct_ById() {
        String result = client.getHct("hct-1", null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("{");
        assertThat(result).contains("\"id\": \"hct-1\"");
        System.out.println("getHct by id: success");
    }

    @Test
    void testGetHct_NotFound() {
        String result = client.getHct("hct-nonexistent", null);

        assertThat(result).contains("\"error\": true");
        assertThat(result).contains("\"code\": \"NOT_FOUND\"");
        System.out.println("getHct not found: error response correct");
    }

    // ========================================
    // GET TDS TESTS
    // ========================================

    @Test
    void testGetTds_ById() {
        String result = client.getTds("tds-1", null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("{");
        assertThat(result).contains("\"id\": \"tds-1\"");
        System.out.println("getTds by id: success");
    }

    @Test
    void testGetTds_NotFound() {
        String result = client.getTds("tds-nonexistent", null);

        assertThat(result).contains("\"error\": true");
        assertThat(result).contains("\"code\": \"NOT_FOUND\"");
        System.out.println("getTds not found: error response correct");
    }

    // ========================================
    // COMPARE MRD TESTS
    // ========================================

    @Test
    void testCompareMrd_ByIds() {
        String result = client.compareMrd("mrd-1,mrd-2", null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");  // Array
        assertThat(result).contains("\"name\":");
        assertThat(result).contains("\"vendor\":");
        // Default metrics should include sensitivity
        assertThat(result).contains("\"sensitivity\":");

        int count = countResults(result);
        assertThat(count).isGreaterThanOrEqualTo(1);  // At least one found
        System.out.println("compareMrd by ids: " + count + " tests compared");
    }

    @Test
    void testCompareMrd_ByNames() {
        // Get two known names
        String searchResult = client.searchMrd(null, null, null, null, null, null, null, "name", 2, null);
        String name1 = extractFirstValue(searchResult, "name");

        if (name1 != null) {
            String result = client.compareMrd(null, name1, null);

            assertThat(result).isNotNull();
            assertThat(result).startsWith("[");
            System.out.println("compareMrd by names: success");
        }
    }

    @Test
    void testCompareMrd_WithCustomMetrics() {
        String result = client.compareMrd("mrd-1,mrd-2", null, "name,vendor,sensitivity");

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");

        // Should have custom metrics
        assertThat(result).contains("\"name\":");
        assertThat(result).contains("\"vendor\":");
        assertThat(result).contains("\"sensitivity\":");

        // Should NOT have non-requested metrics
        assertThat(result).doesNotContain("\"lod\":");
        assertThat(result).doesNotContain("\"initialTat\":");

        System.out.println("compareMrd with custom metrics: correct");
    }

    @Test
    void testCompareMrd_MissingParameter() {
        String result = client.compareMrd(null, null, null);

        assertThat(result).contains("\"error\": true");
        assertThat(result).contains("\"code\": \"MISSING_PARAMETER\"");
        System.out.println("compareMrd missing parameter: error response correct");
    }

    @Test
    void testCompareMrd_NonexistentIds() {
        // When all IDs don't exist, return empty array
        String result = client.compareMrd("mrd-999,mrd-998", null, null);

        assertThat(result).isEqualTo("[]");
        System.out.println("compareMrd nonexistent ids: returns empty array");
    }

    @Test
    void testCompareMrd_PartialMatch() {
        // Mix of existing and non-existing IDs - should return only found
        String result = client.compareMrd("mrd-1,mrd-999", null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");

        // Should have mrd-1 but not mrd-999
        int count = countResults(result);
        assertThat(count).isEqualTo(1);
        System.out.println("compareMrd partial match: returns only found records");
    }

    // ========================================
    // COMPARE ECD TESTS
    // ========================================

    @Test
    void testCompareEcd_ByIds() {
        String result = client.compareEcd("ecd-1,ecd-2", null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        // Default ECD metrics should include testScope
        assertThat(result).contains("\"testScope\":");
        System.out.println("compareEcd by ids: success");
    }

    @Test
    void testCompareEcd_WithCustomMetrics() {
        String result = client.compareEcd("ecd-1,ecd-2", null, "name,vendor,listPrice");

        assertThat(result).isNotNull();
        assertThat(result).contains("\"name\":");
        assertThat(result).contains("\"listPrice\":");
        assertThat(result).doesNotContain("\"testScope\":");
        System.out.println("compareEcd with custom metrics: correct");
    }

    // ========================================
    // COMPARE HCT TESTS
    // ========================================

    @Test
    void testCompareHct_ByIds() {
        String result = client.compareHct("hct-1,hct-2", null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        // Default HCT metrics should include genesAnalyzed
        assertThat(result).contains("\"genesAnalyzed\":");
        System.out.println("compareHct by ids: success");
    }

    @Test
    void testCompareHct_WithCustomMetrics() {
        String result = client.compareHct("hct-1,hct-2", null, "name,vendor,cancerTypesAssessed");

        assertThat(result).isNotNull();
        assertThat(result).contains("\"name\":");
        assertThat(result).contains("\"cancerTypesAssessed\":");
        System.out.println("compareHct with custom metrics: correct");
    }

    // ========================================
    // COMPARE TDS TESTS
    // ========================================

    @Test
    void testCompareTds_ByIds() {
        String result = client.compareTds("tds-1,tds-2", null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        // Default TDS metrics should include genesAnalyzed
        assertThat(result).contains("\"genesAnalyzed\":");
        System.out.println("compareTds by ids: success");
    }

    @Test
    void testCompareTds_WithCustomMetrics() {
        String result = client.compareTds("tds-1,tds-2", null, "name,vendor,fdaCompanionDxCount");

        assertThat(result).isNotNull();
        assertThat(result).contains("\"name\":");
        assertThat(result).contains("\"fdaCompanionDxCount\":");
        assertThat(result).doesNotContain("\"genesAnalyzed\":");
        System.out.println("compareTds with custom metrics: correct");
    }

    @Test
    void testCompareTds_ManyTests() {
        // Compare more than 2 tests
        String result = client.compareTds("tds-1,tds-2,tds-3,tds-4", null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");

        int count = countResults(result);
        assertThat(count).isGreaterThanOrEqualTo(2);
        System.out.println("compareTds many tests: " + count + " tests compared");
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    void testGet_BlankId() {
        String result = client.getMrd("   ", null);

        assertThat(result).contains("\"error\": true");
        assertThat(result).contains("\"code\": \"MISSING_PARAMETER\"");
        System.out.println("get blank id: treated as missing");
    }

    @Test
    void testCompare_SingleId() {
        // Comparing a single test should work (just returns that test)
        String result = client.compareMrd("mrd-1", null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");

        int count = countResults(result);
        assertThat(count).isEqualTo(1);
        System.out.println("compare single id: works correctly");
    }

    @Test
    void testCompare_IdsTakePrecedence() {
        // If both ids and names provided, ids should be used
        // Note: compare returns filtered metrics, so check for name field value, not id
        String result = client.compareMrd("mrd-1", "SomeOtherName", null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        // Should have exactly 1 result (from mrd-1)
        int count = countResults(result);
        assertThat(count).isEqualTo(1);
        System.out.println("compare ids precedence: correct");
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Count the number of result objects in a JSON array string.
     */
    private int countResults(String jsonArray) {
        if (jsonArray == null || jsonArray.equals("[]")) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = jsonArray.indexOf("\"name\":", index)) != -1) {
            count++;
            index++;
        }
        return count;
    }

    /**
     * Extract the first value for a given key from a JSON array.
     */
    private String extractFirstValue(String json, String key) {
        String searchPattern = "\"" + key + "\": \"";
        int start = json.indexOf(searchPattern);
        if (start == -1) {
            return null;
        }
        start += searchPattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) {
            return null;
        }
        return json.substring(start, end);
    }
}
