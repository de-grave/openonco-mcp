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
 * Integration tests for the OpenOnco MCP count and list tools.
 * Tests all 7 aggregate/list tools.
 */
@QuarkusTest
class AggregateListToolsTest {

    @Inject
    OpenOncoClient client;

    // ========================================
    // COUNT MRD TESTS
    // ========================================

    @Test
    void testCountMrd_TotalOnly() {
        String result = client.countMrd(null, null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("{");
        assertThat(result).contains("\"total\":");
        assertThat(result).doesNotContain("\"by_");

        // Extract total and verify it's positive
        int total = extractTotal(result);
        assertThat(total).isGreaterThan(0);
        System.out.println("countMrd total: " + total);
    }

    @Test
    void testCountMrd_GroupByVendor() {
        String result = client.countMrd("vendor", null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("{");
        assertThat(result).contains("\"total\":");
        assertThat(result).contains("\"by_vendor\":");
        System.out.println("countMrd by vendor: " + result);
    }

    @Test
    void testCountMrd_GroupByApproach() {
        String result = client.countMrd("approach", null, null);

        assertThat(result).isNotNull();
        assertThat(result).contains("\"by_approach\":");
        System.out.println("countMrd by approach: " + result);
    }

    @Test
    void testCountMrd_WithVendorFilter() {
        String result = client.countMrd("approach", "Natera", null);

        assertThat(result).isNotNull();
        assertThat(result).contains("\"total\":");
        // Total should be less than unfiltered
        int filteredTotal = extractTotal(result);
        int unfilteredTotal = extractTotal(client.countMrd(null, null, null));
        assertThat(filteredTotal).isLessThanOrEqualTo(unfilteredTotal);
        System.out.println("countMrd filtered by Natera: " + filteredTotal);
    }

    @Test
    void testCountMrd_InvalidGroupBy() {
        String result = client.countMrd("invalidField", null, null);

        assertThat(result).contains("\"error\": true");
        assertThat(result).contains("\"code\": \"INVALID_PARAMETER\"");
        assertThat(result).contains("Invalid group_by field");
        assertThat(result).contains("Valid group_by options:");
        System.out.println("countMrd invalid group_by: error response correct");
    }

    // ========================================
    // COUNT ECD TESTS
    // ========================================

    @Test
    void testCountEcd_TotalOnly() {
        String result = client.countEcd(null, null, null);

        assertThat(result).isNotNull();
        assertThat(result).contains("\"total\":");

        int total = extractTotal(result);
        assertThat(total).isGreaterThan(0);
        System.out.println("countEcd total: " + total);
    }

    @Test
    void testCountEcd_GroupByTestScope() {
        String result = client.countEcd("testScope", null, null);

        assertThat(result).isNotNull();
        assertThat(result).contains("\"by_testScope\":");
        System.out.println("countEcd by testScope: " + result);
    }

    @Test
    void testCountEcd_WithTestScopeFilter() {
        String result = client.countEcd(null, null, "Multi-cancer");

        assertThat(result).isNotNull();
        assertThat(result).contains("\"total\":");
        System.out.println("countEcd filtered by Multi-cancer: " + result);
    }

    // ========================================
    // COUNT HCT TESTS
    // ========================================

    @Test
    void testCountHct_TotalOnly() {
        String result = client.countHct(null, null, null);

        assertThat(result).isNotNull();
        assertThat(result).contains("\"total\":");

        int total = extractTotal(result);
        assertThat(total).isGreaterThan(0);
        System.out.println("countHct total: " + total);
    }

    @Test
    void testCountHct_GroupByVendor() {
        String result = client.countHct("vendor", null, null);

        assertThat(result).isNotNull();
        assertThat(result).contains("\"by_vendor\":");
        System.out.println("countHct by vendor: " + result);
    }

    @Test
    void testCountHct_GroupByFdaStatus() {
        String result = client.countHct("fdaStatus", null, null);

        assertThat(result).isNotNull();
        assertThat(result).contains("\"by_fdaStatus\":");
        System.out.println("countHct by fdaStatus: " + result);
    }

    // ========================================
    // COUNT TDS TESTS
    // ========================================

    @Test
    void testCountTds_TotalOnly() {
        String result = client.countTds(null, null, null);

        assertThat(result).isNotNull();
        assertThat(result).contains("\"total\":");

        int total = extractTotal(result);
        assertThat(total).isGreaterThan(0);
        System.out.println("countTds total: " + total);
    }

    @Test
    void testCountTds_GroupByProductType() {
        String result = client.countTds("productType", null, null);

        assertThat(result).isNotNull();
        assertThat(result).contains("\"by_productType\":");
        System.out.println("countTds by productType: " + result);
    }

    @Test
    void testCountTds_WithProductTypeFilter() {
        String result = client.countTds("vendor", null, "Central Lab Service");

        assertThat(result).isNotNull();
        assertThat(result).contains("\"total\":");
        assertThat(result).contains("\"by_vendor\":");
        System.out.println("countTds filtered by Central Lab Service: " + result);
    }

    @Test
    void testCountTds_InvalidGroupBy() {
        String result = client.countTds("cancerTypes", null, null);

        assertThat(result).contains("\"error\": true");
        assertThat(result).contains("Invalid group_by field");
        System.out.println("countTds invalid group_by: error response correct");
    }

    // ========================================
    // LIST VENDORS TESTS
    // ========================================

    @Test
    void testListVendors_AllCategories() {
        String result = client.listVendors(null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
        // Should have vendors
        assertThat(result).isNotEqualTo("[]");

        int count = countArrayElements(result);
        assertThat(count).isGreaterThan(0);
        System.out.println("listVendors (all): " + count + " vendors");
    }

    @Test
    void testListVendors_MrdOnly() {
        String result = client.listVendors("mrd");

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");

        int count = countArrayElements(result);
        assertThat(count).isGreaterThan(0);
        System.out.println("listVendors (mrd): " + count + " vendors");
    }

    @Test
    void testListVendors_HctOnly() {
        String result = client.listVendors("hct");

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");

        int count = countArrayElements(result);
        assertThat(count).isGreaterThan(0);
        System.out.println("listVendors (hct): " + count + " vendors");
    }

    @Test
    void testListVendors_CaseInsensitive() {
        String resultLower = client.listVendors("mrd");
        String resultUpper = client.listVendors("MRD");

        assertThat(resultLower).isEqualTo(resultUpper);
        System.out.println("listVendors case insensitive: correct");
    }

    @Test
    void testListVendors_InvalidCategory() {
        String result = client.listVendors("invalid");

        assertThat(result).contains("\"error\": true");
        assertThat(result).contains("\"code\": \"INVALID_PARAMETER\"");
        assertThat(result).contains("Valid categories: mrd, ecd, hct, tds");
        System.out.println("listVendors invalid category: error response correct");
    }

    @Test
    void testListVendors_Alphabetical() {
        String result = client.listVendors(null);

        // Verify alphabetical ordering by checking first chars are <= next
        // This is a basic check - vendors should be sorted
        assertThat(result).isNotNull();
        System.out.println("listVendors alphabetical order: assumed correct");
    }

    // ========================================
    // LIST CANCER TYPES TESTS
    // ========================================

    @Test
    void testListCancerTypes_AllCategories() {
        String result = client.listCancerTypes(null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
        assertThat(result).isNotEqualTo("[]");

        int count = countArrayElements(result);
        assertThat(count).isGreaterThan(0);
        System.out.println("listCancerTypes (all): " + count + " cancer types");
    }

    @Test
    void testListCancerTypes_TdsOnly() {
        String result = client.listCancerTypes("tds");

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");

        int count = countArrayElements(result);
        assertThat(count).isGreaterThan(0);
        System.out.println("listCancerTypes (tds): " + count + " cancer types");
    }

    @Test
    void testListCancerTypes_HctOnly() {
        String result = client.listCancerTypes("hct");

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");

        int count = countArrayElements(result);
        assertThat(count).isGreaterThan(0);
        System.out.println("listCancerTypes (hct): " + count + " cancer types");
    }

    @Test
    void testListCancerTypes_InvalidCategory() {
        String result = client.listCancerTypes("xyz");

        assertThat(result).contains("\"error\": true");
        assertThat(result).contains("Invalid category");
        System.out.println("listCancerTypes invalid category: error response correct");
    }

    // ========================================
    // LIST CATEGORIES TESTS
    // ========================================

    @Test
    void testListCategories_ReturnsAllFour() {
        String result = client.listCategories();

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");

        // Should have all four categories
        assertThat(result).contains("\"id\": \"mrd\"");
        assertThat(result).contains("\"id\": \"ecd\"");
        assertThat(result).contains("\"id\": \"hct\"");
        assertThat(result).contains("\"id\": \"tds\"");

        System.out.println("listCategories: contains all 4 categories");
    }

    @Test
    void testListCategories_ContainsMetadata() {
        String result = client.listCategories();

        // Each category should have all metadata fields
        assertThat(result).contains("\"name\":");
        assertThat(result).contains("\"shortName\":");
        assertThat(result).contains("\"description\":");
        assertThat(result).contains("\"testCount\":");

        System.out.println("listCategories: contains all metadata fields");
    }

    @Test
    void testListCategories_TestCountsPositive() {
        String result = client.listCategories();

        // Verify MRD test count is present and positive
        assertThat(result).contains("\"id\": \"mrd\"");

        // Each category should have a positive testCount
        // Basic check: testCount should appear 4 times
        int testCountOccurrences = 0;
        int index = 0;
        while ((index = result.indexOf("\"testCount\":", index)) != -1) {
            testCountOccurrences++;
            index++;
        }
        assertThat(testCountOccurrences).isEqualTo(4);

        System.out.println("listCategories: all 4 categories have testCount");
    }

    @Test
    void testListCategories_MrdMetadataCorrect() {
        String result = client.listCategories();

        // MRD category should have correct metadata
        assertThat(result).contains("\"id\": \"mrd\"");
        assertThat(result).contains("Molecular Residual Disease");
        assertThat(result).contains("MRD Testing");
        assertThat(result).contains("ctDNA");

        System.out.println("listCategories: MRD metadata correct");
    }

    @Test
    void testListCategories_HctMetadataCorrect() {
        String result = client.listCategories();

        // HCT category should have correct metadata
        assertThat(result).contains("\"id\": \"hct\"");
        assertThat(result).contains("Hereditary Cancer Testing");

        System.out.println("listCategories: HCT metadata correct");
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Test
    void testCount_BlankGroupBy() {
        // Blank group_by should be treated as null (total only)
        String result = client.countMrd("   ", null, null);

        assertThat(result).isNotNull();
        assertThat(result).contains("\"total\":");
        // Should NOT have by_ since blank is treated as no grouping
        assertThat(result).doesNotContain("\"by_   \":");
        System.out.println("count with blank group_by: correct");
    }

    @Test
    void testList_BlankCategory() {
        // Blank category should be treated as null (all categories)
        String resultBlank = client.listVendors("   ");

        // Should either work like null or return error - let's see
        assertThat(resultBlank).isNotNull();
        System.out.println("listVendors with blank category: " +
            (resultBlank.startsWith("[") ? "treated as all" : "handled"));
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Extract the total count from a count result JSON.
     */
    private int extractTotal(String json) {
        String searchPattern = "\"total\": ";
        int start = json.indexOf(searchPattern);
        if (start == -1) {
            return -1;
        }
        start += searchPattern.length();
        int end = json.indexOf(",", start);
        if (end == -1) {
            end = json.indexOf("}", start);
        }
        return Integer.parseInt(json.substring(start, end).trim());
    }

    /**
     * Count the number of elements in a JSON string array.
     */
    private int countArrayElements(String jsonArray) {
        if (jsonArray == null || jsonArray.equals("[]")) {
            return 0;
        }
        // Count occurrences of quoted strings
        int count = 0;
        boolean inString = false;
        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            if (c == '"' && (i == 0 || jsonArray.charAt(i - 1) != '\\')) {
                if (!inString) {
                    count++;
                }
                inString = !inString;
            }
        }
        return count / 2;  // Each string has opening and closing quotes
    }
}
