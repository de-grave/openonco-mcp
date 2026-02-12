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

package org.openonco.client;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.openonco.db.DuckDbService;
import org.openonco.db.QueryBuilder;
import org.openonco.db.QueryResult;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Client for OpenOnco data operations.
 * Coordinates between MCP tools and the DuckDB database.
 */
@ApplicationScoped
public class OpenOncoClient {

    @Inject
    DuckDbService dbService;

    // Default comparison metrics per category (from spec)
    private static final List<String> DEFAULT_MRD_METRICS = List.of(
            "name", "vendor", "sensitivity", "specificity", "lod",
            "initialTat", "followUpTat", "fdaStatus", "reimbursement"
    );

    private static final List<String> DEFAULT_ECD_METRICS = List.of(
            "name", "vendor", "testScope", "sensitivity", "specificity",
            "stageISensitivity", "ppv", "npv", "listPrice", "fdaStatus"
    );

    private static final List<String> DEFAULT_HCT_METRICS = List.of(
            "name", "vendor", "genesAnalyzed", "cancerTypesAssessed", "sampleCategory",
            "tat", "listPrice", "fdaStatus"
    );

    private static final List<String> DEFAULT_TDS_METRICS = List.of(
            "name", "vendor", "productType", "genesAnalyzed",
            "fdaCompanionDxCount", "tat", "listPrice", "fdaStatus"
    );

    // Valid group_by fields per category (for count tools)
    private static final Set<String> VALID_MRD_GROUP_BY = Set.of(
            "vendor", "approach", "fdaStatus", "requiresTumorTissue", "reimbursement"
    );
    private static final Set<String> VALID_ECD_GROUP_BY = Set.of(
            "vendor", "testScope", "fdaStatus", "reimbursement"
    );
    private static final Set<String> VALID_HCT_GROUP_BY = Set.of(
            "vendor", "fdaStatus", "reimbursement", "sampleCategory"
    );
    private static final Set<String> VALID_TDS_GROUP_BY = Set.of(
            "vendor", "productType", "fdaStatus", "approach", "reimbursement"
    );

    // Table name mapping for categories
    private static final Map<String, String> CATEGORY_TABLES = Map.of(
            "mrd", "mrd_tests",
            "ecd", "ecd_tests",
            "hct", "hct_tests",
            "tds", "tds_tests"
    );

    // Category metadata for list_categories
    private static final List<CategoryInfo> CATEGORY_METADATA = List.of(
            new CategoryInfo("mrd", "Molecular Residual Disease", "MRD Testing",
                    "Detect residual cancer after treatment via circulating tumor DNA (ctDNA)"),
            new CategoryInfo("ecd", "Early Cancer Detection", "ECD Testing",
                    "Screen for cancer in asymptomatic individuals"),
            new CategoryInfo("hct", "Hereditary Cancer Testing", "HCT Testing",
                    "Identify inherited genetic mutations that increase cancer risk"),
            new CategoryInfo("tds", "Treatment Decision Support", "TDS Testing",
                    "Guide treatment decisions via comprehensive genomic profiling")
    );

    private record CategoryInfo(String id, String name, String shortName, String description) {}

    /**
     * Search MRD (Molecular Residual Disease) tests with optional filters.
     *
     * @param vendor              Filter by vendor name (partial match)
     * @param cancerType          Filter by cancer type (in cancerTypes array)
     * @param approach            Filter by approach ("Tumor-informed" or "Tumor-naive")
     * @param fdaStatus           Filter by FDA status (partial match)
     * @param minSensitivity      Minimum sensitivity percentage
     * @param requiresTumorTissue Filter by tumor tissue requirement (converted to Yes/No)
     * @param clinicalSetting     Filter by clinical setting (in clinicalSettings array)
     * @param fields              Comma-separated list of fields to return
     * @param limit               Maximum records to return
     * @param offset              Records to skip for pagination
     * @return JSON array of matching MRD tests
     */
    public String searchMrd(
            String vendor,
            String cancerType,
            String approach,
            String fdaStatus,
            Double minSensitivity,
            Boolean requiresTumorTissue,
            String clinicalSetting,
            String fields,
            Integer limit,
            Integer offset) {

        Map<String, Object> filters = new LinkedHashMap<>();

        if (vendor != null) filters.put("vendor", vendor);
        if (cancerType != null) filters.put("cancerTypes", cancerType);
        if (approach != null) filters.put("approach", approach);
        if (fdaStatus != null) filters.put("fdaStatus", fdaStatus);
        if (minSensitivity != null) filters.put("min_sensitivity", minSensitivity);
        if (requiresTumorTissue != null) {
            // Convert boolean to "Yes"/"No" string to match data format
            filters.put("requiresTumorTissue", requiresTumorTissue ? "Yes" : "No");
        }
        if (clinicalSetting != null) filters.put("clinicalSettings", clinicalSetting);

        QueryResult query = QueryBuilder.buildSearchQuery(
                "mrd_tests", filters, parseFields(fields), limit, offset);

        return executeAndFormat(query);
    }

    /**
     * Search ECD (Early Cancer Detection) tests with optional filters.
     *
     * @param vendor         Filter by vendor name (partial match)
     * @param cancerType     Filter by cancer type (in cancerTypes array)
     * @param testScope      Filter by test scope ("Single-cancer" or "Multi-cancer")
     * @param fdaStatus      Filter by FDA status (partial match)
     * @param minSensitivity Minimum sensitivity percentage
     * @param minSpecificity Minimum specificity percentage
     * @param maxPrice       Maximum list price
     * @param fields         Comma-separated list of fields to return
     * @param limit          Maximum records to return
     * @param offset         Records to skip for pagination
     * @return JSON array of matching ECD tests
     */
    public String searchEcd(
            String vendor,
            String cancerType,
            String testScope,
            String fdaStatus,
            Double minSensitivity,
            Double minSpecificity,
            Double maxPrice,
            String fields,
            Integer limit,
            Integer offset) {

        Map<String, Object> filters = new LinkedHashMap<>();

        if (vendor != null) filters.put("vendor", vendor);
        if (cancerType != null) filters.put("cancerTypes", cancerType);
        if (testScope != null) filters.put("testScope", testScope);
        if (fdaStatus != null) filters.put("fdaStatus", fdaStatus);
        if (minSensitivity != null) filters.put("min_sensitivity", minSensitivity);
        if (minSpecificity != null) filters.put("min_specificity", minSpecificity);
        if (maxPrice != null) filters.put("max_listPrice", maxPrice);

        QueryResult query = QueryBuilder.buildSearchQuery(
                "ecd_tests", filters, parseFields(fields), limit, offset);

        return executeAndFormat(query);
    }

    /**
     * Search HCT (Hereditary Cancer Testing) tests with optional filters.
     *
     * @param vendor    Filter by vendor name (partial match)
     * @param cancerType Filter by cancer type/syndrome (in cancerTypes array)
     * @param fdaStatus Filter by FDA status (partial match)
     * @param minGenes  Minimum number of genes analyzed
     * @param fields    Comma-separated list of fields to return
     * @param limit     Maximum records to return
     * @param offset    Records to skip for pagination
     * @return JSON array of matching HCT tests
     */
    public String searchHct(
            String vendor,
            String cancerType,
            String fdaStatus,
            Integer minGenes,
            String fields,
            Integer limit,
            Integer offset) {

        Map<String, Object> filters = new LinkedHashMap<>();

        if (vendor != null) filters.put("vendor", vendor);
        if (cancerType != null) filters.put("cancerTypesAssessed", cancerType);
        if (fdaStatus != null) filters.put("fdaStatus", fdaStatus);
        if (minGenes != null) filters.put("min_genesAnalyzed", minGenes);

        QueryResult query = QueryBuilder.buildSearchQuery(
                "hct_tests", filters, parseFields(fields), limit, offset);

        return executeAndFormat(query);
    }

    /**
     * Search TDS (Treatment Decision Support) tests with optional filters.
     *
     * @param vendor         Filter by vendor name (partial match)
     * @param cancerType     Filter by cancer type (in cancerTypes array)
     * @param productType    Filter by product type ("Central Lab Service" or "Laboratory IVD Kit")
     * @param sampleCategory Filter by sample category ("Tissue" or "Blood/Plasma")
     * @param approach       Filter by approach ("Tissue CGP", "Liquid CGP", etc.)
     * @param fdaStatus      Filter by FDA status (partial match)
     * @param minGenes       Minimum number of genes analyzed
     * @param hasFdaCdx      Filter by FDA companion diagnostic approvals
     * @param fields         Comma-separated list of fields to return
     * @param limit          Maximum records to return
     * @param offset         Records to skip for pagination
     * @return JSON array of matching TDS tests
     */
    public String searchTds(
            String vendor,
            String cancerType,
            String productType,
            String sampleCategory,
            String approach,
            String fdaStatus,
            Integer minGenes,
            Boolean hasFdaCdx,
            String fields,
            Integer limit,
            Integer offset) {

        Map<String, Object> filters = new LinkedHashMap<>();

        if (vendor != null) filters.put("vendor", vendor);
        if (cancerType != null) filters.put("cancerTypes", cancerType);
        if (productType != null) filters.put("productType", productType);
        if (sampleCategory != null) filters.put("sampleCategory", sampleCategory);
        if (approach != null) filters.put("approach", approach);
        if (fdaStatus != null) filters.put("fdaStatus", fdaStatus);
        if (minGenes != null) filters.put("min_genesAnalyzed", minGenes);
        if (hasFdaCdx != null) filters.put("has_fda_cdx", hasFdaCdx);

        QueryResult query = QueryBuilder.buildSearchQuery(
                "tds_tests", filters, parseFields(fields), limit, offset);

        return executeAndFormat(query);
    }

    // ========================================
    // PAP (Patient Assistance Programs) TOOLS
    // ========================================

    /**
     * Search PAP (Patient Assistance Programs) with optional filters.
     *
     * @param vendor   Filter by vendor name (partial match)
     * @param medicare Filter by Medicare eligibility
     * @param medicaid Filter by Medicaid eligibility
     * @param fields   Comma-separated list of fields to return
     * @param limit    Maximum records to return
     * @param offset   Records to skip for pagination
     * @return JSON array of matching PAP programs
     */
    public String searchPap(
            String vendor,
            Boolean medicare,
            Boolean medicaid,
            String fields,
            Integer limit,
            Integer offset) {

        Map<String, Object> filters = new LinkedHashMap<>();

        if (vendor != null) filters.put("vendorName", vendor);
        if (medicare != null) filters.put("medicareEligible", medicare);
        if (medicaid != null) filters.put("medicaidEligible", medicaid);

        QueryResult query = QueryBuilder.buildSearchQuery(
                "pap_programs", filters, parseFields(fields), limit, offset);

        return executeAndFormat(query);
    }

    /**
     * Get complete details of a single PAP program by ID or vendor name.
     *
     * @param id   Program ID (e.g., "pap-1")
     * @param name Vendor name (exact match, case-insensitive)
     * @return JSON object with all fields, or error response
     */
    public String getPap(String id, String name) {
        return getPapProgram("pap_programs", "PAP", id, name, "openonco_search_pap");
    }

    /**
     * Get PAP program implementation - uses vendorName instead of name field.
     */
    private String getPapProgram(String table, String category, String id, String name, String searchTool) {
        // Validate: at least one must be provided
        boolean hasId = id != null && !id.isBlank();
        boolean hasName = name != null && !name.isBlank();

        if (!hasId && !hasName) {
            errorResponse("MISSING_PARAMETER",
                    "Either 'id' or 'name' must be provided",
                    "Use " + searchTool + " to find available programs");
        }

        // Build query - prefer id if both provided
        QueryResult query;
        String lookupType;
        String lookupValue;

        if (hasId) {
            query = QueryBuilder.buildGetByIdQuery(table, id.trim());
            lookupType = "id";
            lookupValue = id.trim();
        } else {
            // PAP uses vendorName instead of name
            query = QueryBuilder.buildGetByVendorNameQuery(table, name.trim());
            lookupType = "name";
            lookupValue = name.trim();
        }

        Log.debugf("Get %s program: %s", category, query.sql());

        List<Map<String, Object>> results = dbService.executeQuery(query);

        // Handle not found
        if (results.isEmpty()) {
            errorResponse("NOT_FOUND",
                    "No " + category + " program found with " + lookupType + " '" + lookupValue + "'",
                    "Use " + searchTool + " to find available programs");
        }

        // Return single object (not array)
        return formatAsJsonObject(results.get(0));
    }

    // ========================================
    // GET TOOLS (Detail)
    // ========================================

    /**
     * Get complete details of a single MRD test by ID or name.
     *
     * @param id   Test ID (e.g., "mrd-1")
     * @param name Test name (e.g., "Signatera")
     * @return JSON object with all fields, or error response
     */
    public String getMrd(String id, String name) {
        return getTest("mrd_tests", "MRD", id, name, "openonco_search_mrd");
    }

    /**
     * Get complete details of a single ECD test by ID or name.
     *
     * @param id   Test ID (e.g., "ecd-1")
     * @param name Test name (e.g., "Galleri")
     * @return JSON object with all fields, or error response
     */
    public String getEcd(String id, String name) {
        return getTest("ecd_tests", "ECD", id, name, "openonco_search_ecd");
    }

    /**
     * Get complete details of a single HCT test by ID or name.
     *
     * @param id   Test ID (e.g., "hct-1")
     * @param name Test name
     * @return JSON object with all fields, or error response
     */
    public String getHct(String id, String name) {
        return getTest("hct_tests", "HCT", id, name, "openonco_search_hct");
    }

    /**
     * Get complete details of a single TDS test by ID or name.
     *
     * @param id   Test ID (e.g., "tds-1")
     * @param name Test name (e.g., "FoundationOne CDx")
     * @return JSON object with all fields, or error response
     */
    public String getTds(String id, String name) {
        return getTest("tds_tests", "TDS", id, name, "openonco_search_tds");
    }

    /**
     * Generic get test implementation.
     */
    private String getTest(String table, String category, String id, String name, String searchTool) {
        // Validate: at least one must be provided
        boolean hasId = id != null && !id.isBlank();
        boolean hasName = name != null && !name.isBlank();

        if (!hasId && !hasName) {
            errorResponse("MISSING_PARAMETER",
                    "Either 'id' or 'name' must be provided",
                    "Use " + searchTool + " to find available tests");
        }

        // Build query - prefer id if both provided
        QueryResult query;
        String lookupType;
        String lookupValue;

        if (hasId) {
            query = QueryBuilder.buildGetByIdQuery(table, id.trim());
            lookupType = "id";
            lookupValue = id.trim();
        } else {
            query = QueryBuilder.buildGetByNameQuery(table, name.trim());
            lookupType = "name";
            lookupValue = name.trim();
        }

        Log.debugf("Get %s test: %s", category, query.sql());

        List<Map<String, Object>> results = dbService.executeQuery(query);

        // Handle not found
        if (results.isEmpty()) {
            errorResponse("NOT_FOUND",
                    "No " + category + " test found with " + lookupType + " '" + lookupValue + "'",
                    "Use " + searchTool + " to find available tests");
        }

        // Return single object (not array)
        return formatAsJsonObject(results.get(0));
    }

    // ========================================
    // COMPARE TOOLS
    // ========================================

    /**
     * Compare multiple MRD tests side-by-side on specified metrics.
     *
     * @param ids     Comma-separated test IDs
     * @param names   Comma-separated test names
     * @param metrics Comma-separated metrics to compare (null for defaults)
     * @return JSON array with each test showing comparison metrics
     */
    public String compareMrd(String ids, String names, String metrics) {
        return compareTests("mrd_tests", "MRD", ids, names, metrics, DEFAULT_MRD_METRICS);
    }

    /**
     * Compare multiple ECD tests side-by-side on specified metrics.
     *
     * @param ids     Comma-separated test IDs
     * @param names   Comma-separated test names
     * @param metrics Comma-separated metrics to compare (null for defaults)
     * @return JSON array with each test showing comparison metrics
     */
    public String compareEcd(String ids, String names, String metrics) {
        return compareTests("ecd_tests", "ECD", ids, names, metrics, DEFAULT_ECD_METRICS);
    }

    /**
     * Compare multiple HCT tests side-by-side on specified metrics.
     *
     * @param ids     Comma-separated test IDs
     * @param names   Comma-separated test names
     * @param metrics Comma-separated metrics to compare (null for defaults)
     * @return JSON array with each test showing comparison metrics
     */
    public String compareHct(String ids, String names, String metrics) {
        return compareTests("hct_tests", "HCT", ids, names, metrics, DEFAULT_HCT_METRICS);
    }

    /**
     * Compare multiple TDS tests side-by-side on specified metrics.
     *
     * @param ids     Comma-separated test IDs
     * @param names   Comma-separated test names
     * @param metrics Comma-separated metrics to compare (null for defaults)
     * @return JSON array with each test showing comparison metrics
     */
    public String compareTds(String ids, String names, String metrics) {
        return compareTests("tds_tests", "TDS", ids, names, metrics, DEFAULT_TDS_METRICS);
    }

    /**
     * Generic compare tests implementation.
     */
    private String compareTests(String table, String category, String ids, String names,
                                String metrics, List<String> defaultMetrics) {
        // Validate: at least one list must be provided
        boolean hasIds = ids != null && !ids.isBlank();
        boolean hasNames = names != null && !names.isBlank();

        if (!hasIds && !hasNames) {
            errorResponse("MISSING_PARAMETER",
                    "Either 'ids' or 'names' must be provided",
                    "Provide comma-separated list like 'mrd-1,mrd-2' or 'Signatera,Guardant Reveal'");
        }

        // Build query - prefer ids if both provided
        QueryResult query;
        if (hasIds) {
            List<String> idList = parseCommaSeparated(ids);
            query = QueryBuilder.buildGetByIdsQuery(table, idList);
        } else {
            List<String> nameList = parseCommaSeparated(names);
            query = QueryBuilder.buildGetByNamesQuery(table, nameList);
        }

        Log.debugf("Compare %s tests: %s", category, query.sql());

        List<Map<String, Object>> results = dbService.executeQuery(query);

        // Determine which metrics to include
        List<String> metricsToShow = (metrics != null && !metrics.isBlank())
                ? parseCommaSeparated(metrics)
                : defaultMetrics;

        // Filter results to only include requested metrics
        List<Map<String, Object>> filteredResults = results.stream()
                .map(row -> filterToMetrics(row, metricsToShow))
                .toList();

        return formatAsJsonArray(filteredResults);
    }

    /**
     * Filter a row to only include specified metrics.
     */
    private Map<String, Object> filterToMetrics(Map<String, Object> row, List<String> metrics) {
        Map<String, Object> filtered = new LinkedHashMap<>();
        for (String metric : metrics) {
            if (row.containsKey(metric)) {
                filtered.put(metric, row.get(metric));
            }
        }
        return filtered;
    }

    /**
     * Parse comma-separated string into a list of trimmed values.
     */
    private List<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Create an error response JSON object.
     */
    private void errorResponse(String code, String message, String suggestion) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"error\": true, ");
        json.append("\"code\": \"").append(code).append("\", ");
        json.append("\"message\": \"").append(escapeJson(message)).append("\"");
        if (suggestion != null) {
            json.append(", \"suggestion\": \"").append(escapeJson(suggestion)).append("\"");
        }
        json.append("}");
        throw new RuntimeException(json.toString());
    }

    /**
     * Parse comma-separated fields string into a list.
     *
     * @param fields Comma-separated field names, or null for all fields
     * @return List of field names, or null for all fields
     */
    private List<String> parseFields(String fields) {
        if (fields == null || fields.isBlank()) {
            return null;  // null = SELECT *
        }
        return Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(f -> !f.isEmpty())
                .toList();
    }

    /**
     * Execute a query and format results as JSON array.
     *
     * @param query The query to execute
     * @return JSON array string of results
     */
    private String executeAndFormat(QueryResult query) {
        Log.debugf("Executing query: %s with params: %s",
                query.sql(), Arrays.toString(query.params()));

        List<Map<String, Object>> results = dbService.executeQuery(query);
        return formatAsJsonArray(results);
    }

    /**
     * Format a list of result maps as a JSON array.
     *
     * @param results List of row maps
     * @return JSON array string
     */
    private String formatAsJsonArray(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < results.size(); i++) {
            json.append("  ");
            json.append(formatAsJsonObject(results.get(i)));
            if (i < results.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Format a single result map as a JSON object.
     *
     * @param row Map of column names to values
     * @return JSON object string
     */
    private String formatAsJsonObject(Map<String, Object> row) {
        StringBuilder json = new StringBuilder("{");
        int fieldCount = 0;

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (fieldCount > 0) {
                json.append(", ");
            }
            json.append("\"").append(entry.getKey()).append("\": ");
            json.append(formatJsonValue(entry.getValue()));
            fieldCount++;
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Format a value as JSON.
     *
     * @param value The value to format
     * @return JSON representation
     */
    private String formatJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder arr = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) arr.append(", ");
                arr.append(formatJsonValue(list.get(i)));
            }
            arr.append("]");
            return arr.toString();
        }
        // Fallback: treat as string
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    /**
     * Escape special characters in JSON strings.
     *
     * @param text The text to escape
     * @return Escaped text safe for JSON
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ========================================
    // COUNT TOOLS (Aggregate)
    // ========================================

    /**
     * Count MRD tests with optional grouping and filtering.
     *
     * @param groupBy       Field to group by (null for total count only)
     * @param filterVendor  Filter by vendor before counting
     * @param filterApproach Filter by approach before counting
     * @return JSON with total count and optional grouped counts
     */
    public String countMrd(String groupBy, String filterVendor, String filterApproach) {
        Map<String, Object> filters = new LinkedHashMap<>();
        if (filterVendor != null) filters.put("vendor", filterVendor);
        if (filterApproach != null) filters.put("approach", filterApproach);

        return countTests("mrd_tests", "MRD", groupBy, filters, VALID_MRD_GROUP_BY);
    }

    /**
     * Count ECD tests with optional grouping and filtering.
     *
     * @param groupBy        Field to group by (null for total count only)
     * @param filterVendor   Filter by vendor before counting
     * @param filterTestScope Filter by test scope before counting
     * @return JSON with total count and optional grouped counts
     */
    public String countEcd(String groupBy, String filterVendor, String filterTestScope) {
        Map<String, Object> filters = new LinkedHashMap<>();
        if (filterVendor != null) filters.put("vendor", filterVendor);
        if (filterTestScope != null) filters.put("testScope", filterTestScope);

        return countTests("ecd_tests", "ECD", groupBy, filters, VALID_ECD_GROUP_BY);
    }

    /**
     * Count HCT tests with optional grouping and filtering.
     *
     * @param groupBy         Field to group by (null for total count only)
     * @param filterVendor    Filter by vendor before counting
     * @param filterFdaStatus Filter by FDA status before counting
     * @return JSON with total count and optional grouped counts
     */
    public String countHct(String groupBy, String filterVendor, String filterFdaStatus) {
        Map<String, Object> filters = new LinkedHashMap<>();
        if (filterVendor != null) filters.put("vendor", filterVendor);
        if (filterFdaStatus != null) filters.put("fdaStatus", filterFdaStatus);

        return countTests("hct_tests", "HCT", groupBy, filters, VALID_HCT_GROUP_BY);
    }

    /**
     * Count TDS tests with optional grouping and filtering.
     *
     * @param groupBy           Field to group by (null for total count only)
     * @param filterVendor      Filter by vendor before counting
     * @param filterProductType Filter by product type before counting
     * @return JSON with total count and optional grouped counts
     */
    public String countTds(String groupBy, String filterVendor, String filterProductType) {
        Map<String, Object> filters = new LinkedHashMap<>();
        if (filterVendor != null) filters.put("vendor", filterVendor);
        if (filterProductType != null) filters.put("productType", filterProductType);

        return countTests("tds_tests", "TDS", groupBy, filters, VALID_TDS_GROUP_BY);
    }

    /**
     * Generic count implementation.
     */
    private String countTests(String table, String category, String groupBy,
                              Map<String, Object> filters, Set<String> validGroupByFields) {
        // Validate group_by if provided
        if (groupBy != null && !groupBy.isBlank()) {
            String trimmedGroupBy = groupBy.trim();
            if (!validGroupByFields.contains(trimmedGroupBy)) {
                String validOptions = validGroupByFields.stream()
                        .sorted()
                        .collect(Collectors.joining(", "));
                errorResponse("INVALID_PARAMETER",
                        "Invalid group_by field '" + trimmedGroupBy + "' for " + category + " tests",
                        "Valid group_by options: " + validOptions);
            }
        }

        // Get total count
        QueryResult countQuery = QueryBuilder.buildCountQuery(table, filters);
        int total = dbService.executeCount(countQuery);

        // Build result
        StringBuilder json = new StringBuilder("{");
        json.append("\"total\": ").append(total);

        // If group_by specified, get grouped counts
        if (groupBy != null && !groupBy.isBlank()) {
            String trimmedGroupBy = groupBy.trim();
            QueryResult groupQuery = QueryBuilder.buildGroupByCountQuery(table, trimmedGroupBy, filters);

            Log.debugf("Count %s grouped by %s: %s", category, trimmedGroupBy, groupQuery.sql());

            List<Map<String, Object>> groupedResults = dbService.executeQuery(groupQuery);

            json.append(", \"by_").append(trimmedGroupBy).append("\": {");

            for (int i = 0; i < groupedResults.size(); i++) {
                Map<String, Object> row = groupedResults.get(i);
                Object groupValue = row.get(trimmedGroupBy);
                Object count = row.get("count");

                if (i > 0) json.append(", ");
                json.append("\"").append(escapeJson(formatGroupValue(groupValue))).append("\": ");
                json.append(count);
            }

            json.append("}");
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Format a group value for JSON output.
     */
    private String formatGroupValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        return value.toString();
    }

    // ========================================
    // LIST TOOLS
    // ========================================

    /**
     * List all vendors across all categories or for a specific category.
     *
     * @param category Filter by category ("mrd", "ecd", "hct", "tds") or null for all
     * @return JSON array of distinct vendor names, sorted alphabetically
     */
    public String listVendors(String category) {
        return listDistinctField("vendor", category, false);
    }

    /**
     * List all cancer types across all categories or for a specific category.
     *
     * @param category Filter by category ("mrd", "ecd", "hct", "tds") or null for all
     * @return JSON array of distinct cancer types, sorted alphabetically
     */
    public String listCancerTypes(String category) {
        // HCT uses cancerTypesAssessed, others use cancerTypes
        if (category != null && !category.isBlank()) {
            String normalizedCategory = category.trim().toLowerCase();
            if (!CATEGORY_TABLES.containsKey(normalizedCategory)) {
                errorResponse("INVALID_PARAMETER",
                        "Invalid category '" + category + "'",
                        "Valid categories: mrd, ecd, hct, tds");
            }
            String field = normalizedCategory.equals("hct") ? "cancerTypesAssessed" : "cancerTypes";
            String table = CATEGORY_TABLES.get(normalizedCategory);
            List<String> values = getDistinctValues(table, field, true);
            return formatAsStringArray(values);
        }

        // Query all tables with their respective field names
        List<String> values = new java.util.ArrayList<>();
        for (Map.Entry<String, String> entry : CATEGORY_TABLES.entrySet()) {
            String cat = entry.getKey();
            String table = entry.getValue();
            String field = cat.equals("hct") ? "cancerTypesAssessed" : "cancerTypes";
            values.addAll(getDistinctValues(table, field, true));
        }
        return formatAsStringArray(values.stream().distinct().sorted().toList());
    }

    /**
     * Generic implementation for listing distinct field values.
     *
     * @param field      The field to get distinct values for
     * @param category   Category filter (null for all)
     * @param isArrayField Whether the field is an array that needs unnesting
     * @return JSON array of distinct values
     */
    private String listDistinctField(String field, String category, boolean isArrayField) {
        // Validate category if provided
        if (category != null && !category.isBlank()) {
            String normalizedCategory = category.trim().toLowerCase();
            if (!CATEGORY_TABLES.containsKey(normalizedCategory)) {
                errorResponse("INVALID_PARAMETER",
                        "Invalid category '" + category + "'",
                        "Valid categories: mrd, ecd, hct, tds");
            }
        }

        List<String> values;

        if (category != null && !category.isBlank()) {
            // Query single table
            String table = CATEGORY_TABLES.get(category.trim().toLowerCase());
            values = getDistinctValues(table, field, isArrayField);
        } else {
            // Query all tables and union results
            values = CATEGORY_TABLES.values().stream()
                    .flatMap(table -> getDistinctValues(table, field, isArrayField).stream())
                    .distinct()
                    .sorted()
                    .toList();
        }

        return formatAsStringArray(values);
    }

    /**
     * Get distinct values for a field from a table.
     */
    private List<String> getDistinctValues(String table, String field, boolean isArrayField) {
        QueryResult query = QueryBuilder.buildDistinctQuery(table, field);
        Log.debugf("List distinct %s from %s: %s", field, table, query.sql());

        List<Map<String, Object>> results = dbService.executeQuery(query);

        // Extract values - handle array unnest (returns "value" column) vs regular (returns field name)
        String columnName = isArrayField ? "value" : field;

        return results.stream()
                .map(row -> row.get(columnName))
                .filter(v -> v != null)
                .map(Object::toString)
                .sorted()
                .toList();
    }

    /**
     * List all test categories with metadata and test counts.
     *
     * @return JSON array of category objects with id, name, shortName, description, testCount
     */
    public String listCategories() {
        StringBuilder json = new StringBuilder("[\n");

        for (int i = 0; i < CATEGORY_METADATA.size(); i++) {
            CategoryInfo info = CATEGORY_METADATA.get(i);
            String table = CATEGORY_TABLES.get(info.id());
            int testCount = dbService.getTableRowCount(table);

            json.append("  {");
            json.append("\"id\": \"").append(info.id()).append("\", ");
            json.append("\"name\": \"").append(escapeJson(info.name())).append("\", ");
            json.append("\"shortName\": \"").append(escapeJson(info.shortName())).append("\", ");
            json.append("\"description\": \"").append(escapeJson(info.description())).append("\", ");
            json.append("\"testCount\": ").append(testCount);
            json.append("}");

            if (i < CATEGORY_METADATA.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("]");
        return json.toString();
    }

    /**
     * Format a list of strings as a JSON array.
     */
    private String formatAsStringArray(List<String> values) {
        if (values.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) json.append(", ");
            json.append("\"").append(escapeJson(values.get(i))).append("\"");
        }
        json.append("]");
        return json.toString();
    }
}
