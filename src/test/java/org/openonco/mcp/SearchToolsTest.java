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
 * Integration tests for the OpenOnco MCP search tools.
 * Tests all 4 search tools with various filter combinations.
 */
@QuarkusTest
class SearchToolsTest {

    @Inject
    OpenOncoClient client;

    // ========================================
    // MRD SEARCH TESTS
    // ========================================

    @Test
    void testSearchMrd_NoFilters() {
        String result = client.searchMrd(null, null, null, null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        assertThat(result).contains("\"id\":");
        assertThat(result).contains("\"name\":");
        assertThat(result).contains("\"vendor\":");
        System.out.println("MRD search (no filters): " + countResults(result) + " results");
    }

    @Test
    void testSearchMrd_ByVendor() {
        String result = client.searchMrd("Natera", null, null, null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        // Should only contain Natera if results exist
        if (!result.equals("[]")) {
            assertThat(result.toLowerCase()).contains("natera");
        }
        System.out.println("MRD search (vendor=Natera): " + countResults(result) + " results");
    }

    @Test
    void testSearchMrd_ByApproach() {
        String result = client.searchMrd(null, null, "Tumor-informed", null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result.toLowerCase()).contains("tumor-informed");
        }
        System.out.println("MRD search (approach=Tumor-informed): " + countResults(result) + " results");
    }

    @Test
    void testSearchMrd_ByCancerType() {
        String result = client.searchMrd(null, "Colorectal", null, null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result.toLowerCase()).contains("colorectal");
        }
        System.out.println("MRD search (cancer_type=Colorectal): " + countResults(result) + " results");
    }

    @Test
    void testSearchMrd_ByMinSensitivity() {
        String result = client.searchMrd(null, null, null, null, 90.0, null, null, null, null, null);

        assertThat(result).isNotNull();
        System.out.println("MRD search (min_sensitivity=90): " + countResults(result) + " results");
    }

    @Test
    void testSearchMrd_ByRequiresTumorTissue() {
        String result = client.searchMrd(null, null, null, null, null, true, null, null, null, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result).contains("\"requiresTumorTissue\": \"Yes\"");
        }
        System.out.println("MRD search (requires_tumor_tissue=true): " + countResults(result) + " results");
    }

    @Test
    void testSearchMrd_ByClinicalSetting() {
        String result = client.searchMrd(null, null, null, null, null, null, "Post-Surgery", null, null, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result).contains("Post-Surgery");
        }
        System.out.println("MRD search (clinical_setting=Post-Surgery): " + countResults(result) + " results");
    }

    @Test
    void testSearchMrd_WithSpecificFields() {
        String result = client.searchMrd(null, null, null, null, null, null, null, "id,name,vendor", 5, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            // Should have the requested fields
            assertThat(result).contains("\"id\":");
            assertThat(result).contains("\"name\":");
            assertThat(result).contains("\"vendor\":");
            // Should NOT have other fields like sensitivity
            assertThat(result).doesNotContain("\"sensitivity\":");
        }
        System.out.println("MRD search (fields=id,name,vendor): " + countResults(result) + " results");
    }

    @Test
    void testSearchMrd_WithPagination() {
        String result1 = client.searchMrd(null, null, null, null, null, null, null, "id,name", 3, 0);
        String result2 = client.searchMrd(null, null, null, null, null, null, null, "id,name", 3, 3);

        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();

        // Results should be different (unless we have <= 3 records)
        int count1 = countResults(result1);
        int count2 = countResults(result2);
        System.out.println("MRD search pagination: page1=" + count1 + ", page2=" + count2 + " results");
    }

    @Test
    void testSearchMrd_MultipleFilters() {
        String result = client.searchMrd("Natera", null, "Tumor-informed", null, 80.0, null, null, null, null, null);

        assertThat(result).isNotNull();
        System.out.println("MRD search (vendor=Natera, approach=Tumor-informed, min_sens=80): " + countResults(result) + " results");
    }

    // ========================================
    // ECD SEARCH TESTS
    // ========================================

    @Test
    void testSearchEcd_NoFilters() {
        String result = client.searchEcd(null, null, null, null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        System.out.println("ECD search (no filters): " + countResults(result) + " results");
    }

    @Test
    void testSearchEcd_ByVendor() {
        String result = client.searchEcd("GRAIL", null, null, null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result.toUpperCase()).contains("GRAIL");
        }
        System.out.println("ECD search (vendor=GRAIL): " + countResults(result) + " results");
    }

    @Test
    void testSearchEcd_ByTestScope() {
        String result = client.searchEcd(null, null, "Multi-cancer", null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result).contains("Multi-cancer");
        }
        System.out.println("ECD search (test_scope=Multi-cancer): " + countResults(result) + " results");
    }

    @Test
    void testSearchEcd_ByMaxPrice() {
        String result = client.searchEcd(null, null, null, null, null, null, 1000.0, null, null, null);

        assertThat(result).isNotNull();
        System.out.println("ECD search (max_price=1000): " + countResults(result) + " results");
    }

    @Test
    void testSearchEcd_ByMinSpecificity() {
        String result = client.searchEcd(null, null, null, null, null, 99.0, null, null, null, null);

        assertThat(result).isNotNull();
        System.out.println("ECD search (min_specificity=99): " + countResults(result) + " results");
    }

    @Test
    void testSearchEcd_WithSpecificFields() {
        String result = client.searchEcd(null, null, null, null, null, null, null, "id,name,testScope,listPrice", 10, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result).contains("\"id\":");
            assertThat(result).contains("\"name\":");
        }
        System.out.println("ECD search (with fields): " + countResults(result) + " results");
    }

    // ========================================
    // HCT SEARCH TESTS
    // ========================================

    @Test
    void testSearchHct_NoFilters() {
        String result = client.searchHct(null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        System.out.println("HCT search (no filters): " + countResults(result) + " results");
    }

    @Test
    void testSearchHct_ByVendor() {
        String result = client.searchHct("Myriad", null, null, null, null, null, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result.toLowerCase()).contains("myriad");
        }
        System.out.println("HCT search (vendor=Myriad): " + countResults(result) + " results");
    }

    @Test
    void testSearchHct_ByCancerType() {
        String result = client.searchHct(null, "Breast", null, null, null, null, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result.toLowerCase()).contains("breast");
        }
        System.out.println("HCT search (cancer_type=Breast): " + countResults(result) + " results");
    }

    @Test
    void testSearchHct_ByMinGenes() {
        String result = client.searchHct(null, null, null, 30, null, null, null);

        assertThat(result).isNotNull();
        System.out.println("HCT search (min_genes=30): " + countResults(result) + " results");
    }

    @Test
    void testSearchHct_WithSpecificFields() {
        String result = client.searchHct(null, null, null, null, "id,name,vendor,genesAnalyzed", 10, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result).contains("\"id\":");
            assertThat(result).contains("\"name\":");
            assertThat(result).contains("\"vendor\":");
        }
        System.out.println("HCT search (with fields): " + countResults(result) + " results");
    }

    // ========================================
    // TDS SEARCH TESTS
    // ========================================

    @Test
    void testSearchTds_NoFilters() {
        String result = client.searchTds(null, null, null, null, null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        System.out.println("TDS search (no filters): " + countResults(result) + " results");
    }

    @Test
    void testSearchTds_ByVendor() {
        String result = client.searchTds("Foundation", null, null, null, null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result.toLowerCase()).contains("foundation");
        }
        System.out.println("TDS search (vendor=Foundation): " + countResults(result) + " results");
    }

    @Test
    void testSearchTds_BySampleCategory() {
        String result = client.searchTds(null, null, null, "Tissue", null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result).contains("Tissue");
        }
        System.out.println("TDS search (sample_category=Tissue): " + countResults(result) + " results");
    }

    @Test
    void testSearchTds_ByProductType() {
        String result = client.searchTds(null, null, "Central Lab Service", null, null, null, null, null, null, null, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result).contains("Central Lab Service");
        }
        System.out.println("TDS search (product_type=Central Lab Service): " + countResults(result) + " results");
    }

    @Test
    void testSearchTds_ByMinGenes() {
        String result = client.searchTds(null, null, null, null, null, null, 300, null, null, null, null);

        assertThat(result).isNotNull();
        System.out.println("TDS search (min_genes=300): " + countResults(result) + " results");
    }

    @Test
    void testSearchTds_ByHasFdaCdx() {
        String resultWithCdx = client.searchTds(null, null, null, null, null, null, null, true, null, null, null);
        String resultWithoutCdx = client.searchTds(null, null, null, null, null, null, null, false, null, null, null);

        assertThat(resultWithCdx).isNotNull();
        assertThat(resultWithoutCdx).isNotNull();

        System.out.println("TDS search: with FDA CDx=" + countResults(resultWithCdx) +
                ", without FDA CDx=" + countResults(resultWithoutCdx) + " results");
    }

    @Test
    void testSearchTds_MultipleFilters() {
        String result = client.searchTds("Foundation", null, null, "Tissue", null, null, 300, true, null, null, null);

        assertThat(result).isNotNull();
        System.out.println("TDS search (vendor=Foundation, sample=Tissue, min_genes=300, has_cdx=true): " + countResults(result) + " results");
    }

    @Test
    void testSearchTds_WithSpecificFields() {
        String result = client.searchTds(null, null, null, null, null, null, null, null, "id,name,vendor,genesAnalyzed", 10, null);

        assertThat(result).isNotNull();
        if (!result.equals("[]")) {
            assertThat(result).contains("\"id\":");
            assertThat(result).contains("\"name\":");
            assertThat(result).contains("\"vendor\":");
            assertThat(result).contains("\"genesAnalyzed\":");
        }
        System.out.println("TDS search (with fields): " + countResults(result) + " results");
    }

    // ========================================
    // EDGE CASE TESTS
    // ========================================

    @Test
    void testSearch_EmptyResults() {
        // Search for a vendor that doesn't exist
        String result = client.searchMrd("NonExistentVendor12345", null, null, null, null, null, null, null, null, null);

        assertThat(result).isEqualTo("[]");
        System.out.println("Empty result test passed");
    }

    @Test
    void testSearch_LimitZero() {
        // Limit of 0 should use default (50)
        String result = client.searchMrd(null, null, null, null, null, null, null, null, 0, null);

        assertThat(result).isNotNull();
        // Should still return results with default limit
        System.out.println("Limit=0 test: " + countResults(result) + " results (used default)");
    }

    @Test
    void testSearch_LargeLimit() {
        // Large limit should be capped at 500
        String result = client.searchMrd(null, null, null, null, null, null, null, null, 1000, null);

        assertThat(result).isNotNull();
        int count = countResults(result);
        assertThat(count).isLessThanOrEqualTo(500);
        System.out.println("Large limit test: " + count + " results (max 500)");
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
        // Count occurrences of "id": which appears once per record
        int count = 0;
        int index = 0;
        while ((index = jsonArray.indexOf("\"id\":", index)) != -1) {
            count++;
            index++;
        }
        return count;
    }
}
