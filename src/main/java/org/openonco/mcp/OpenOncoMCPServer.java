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

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.openonco.client.OpenOncoClient;
import org.openonco.mcp.util.McpResponse;

import java.util.List;

/**
 * MCP Server exposing OpenOnco oncology diagnostic test data tools.
 * <p>
 * Provides tools for AI assistants to search, filter, and analyze
 * oncology diagnostic tests across four categories:
 * <ul>
 *   <li>MRD - Molecular Residual Disease</li>
 *   <li>ECD - Early Cancer Detection</li>
 *   <li>HCT - Hereditary Cancer Testing</li>
 *   <li>TDS - Treatment Decision Support</li>
 * </ul>
 */
@SuppressWarnings("unused")
@ApplicationScoped
public class OpenOncoMCPServer {

    @Inject
    OpenOncoClient client;

    @Startup
    void init() {
        Log.info("Starting OpenOnco MCP server...");
    }

    /**
     * Wraps a tool call. Returns a proper ToolResponse on success.
     * Throws ToolCallException on failure so Quarkus sets isError=true.
     */
    private ToolResponse safeExecute(String toolName, java.util.function.Supplier<String> operation) {
        try {
            String jsonResult = operation.get();
            // Quarkus ToolResponse: (isError=false, contents, structuredContent, metadata)
            return new ToolResponse(
                false,
                List.of(new TextContent(jsonResult)),
                null,
                null
            );
        } catch (Exception e) {
            Log.errorf("Error in %s: %s", toolName, e.getMessage());
            throw McpResponse.error(e);
        }
    }

    // ========================================
    // SEARCH TOOLS
    // ========================================

    @Tool(
        title = "openonco_search_mrd",
        description = """
            Search and filter MRD (Molecular Residual Disease) tests.

            MRD tests detect residual cancer after treatment via ctDNA in blood samples.
            Use this when users ask about post-treatment monitoring, recurrence detection,
            or comparing ctDNA-based MRD assays from different vendors.

            Tip: Use openonco_list_vendors or openonco_list_cancer_types first to discover
            available filter values. Use openonco_get_mrd with a test ID for full details.

            Returns JSON array of matching tests. Empty array [] if no matches.
            Default: 50 results, max: 500. Use 'fields' to limit response size.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_search_mrd",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_search_mrd(
            @ToolArg(description = "Filter by vendor/company name (partial match, case-insensitive). " +
                    "Examples: 'Natera', 'Guardant', 'Foundation'",
                    required = false)
            String vendor,

            @ToolArg(description = "Filter by cancer type (matches within cancerTypes array). " +
                    "Examples: 'Colorectal', 'Breast', 'NSCLC', 'Multi-solid'",
                    required = false)
            String cancer_type,

            @ToolArg(description = "Filter by testing approach. " +
                    "Values: 'Tumor-informed' (requires tumor tissue) or 'Tumor-naive' (blood-only)",
                    required = false)
            String approach,

            @ToolArg(description = "Filter by FDA regulatory status (partial match). " +
                    "Examples: 'FDA', 'CLIA', 'Breakthrough'",
                    required = false)
            String fda_status,

            @ToolArg(description = "Minimum sensitivity percentage (0-100). " +
                    "Example: 90 for tests with ≥90% sensitivity",
                    required = false)
            Double min_sensitivity,

            @ToolArg(description = "Filter by tumor tissue requirement. " +
                    "true = requires tumor tissue, false = does not require tumor tissue",
                    required = false)
            Boolean requires_tumor_tissue,

            @ToolArg(description = "Filter by clinical setting (matches within clinicalSettings array). " +
                    "Values: 'Post-Surgery', 'Surveillance', 'Post-Adjuvant'",
                    required = false)
            String clinical_setting,

            @ToolArg(description = "Comma-separated list of fields to return. " +
                    "Example: 'id,name,vendor,sensitivity,specificity'. " +
                    "Returns all fields if not specified.",
                    required = false)
            String fields,

            @ToolArg(description = "Maximum number of records to return (default: 50, max: 500)",
                    required = false)
            Integer limit,

            @ToolArg(description = "Number of records to skip for pagination (default: 0)",
                    required = false)
            Integer offset
    ) {
        return safeExecute("openonco_search_mrd", () ->
                client.searchMrd(vendor, cancer_type, approach, fda_status,
                        min_sensitivity, requires_tumor_tissue, clinical_setting,
                        fields, limit, offset));
    }

    @Tool(
        title = "openonco_search_ecd",
        description = """
            Search and filter ECD (Early Cancer Detection) tests.

            ECD tests screen for cancer in asymptomatic individuals before symptoms appear.
            Use this when users ask about cancer screening, MCED (multi-cancer early detection),
            liquid biopsy screening, or comparing screening test costs and accuracy.

            Tip: Filter by test_scope='Multi-cancer' for MCED tests like Galleri.
            Use openonco_get_ecd with a test ID for full details including stage sensitivity.

            Returns JSON array of matching tests. Empty array [] if no matches.
            Default: 50 results, max: 500. Use 'fields' to limit response size.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_search_ecd",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_search_ecd(
            @ToolArg(description = "Filter by vendor/company name (partial match, case-insensitive). " +
                    "Examples: 'GRAIL', 'Exact Sciences', 'Freenome'",
                    required = false)
            String vendor,

            @ToolArg(description = "Filter by cancer type (matches within cancerTypes array). " +
                    "Examples: 'Colorectal', 'Lung', 'Multi-cancer'",
                    required = false)
            String cancer_type,

            @ToolArg(description = "Filter by test scope. " +
                    "Values: 'Single-cancer' (screens for one cancer type) or 'Multi-cancer' (MCED)",
                    required = false)
            String test_scope,

            @ToolArg(description = "Filter by FDA regulatory status (partial match). " +
                    "Examples: 'FDA', 'CLIA', 'approved'",
                    required = false)
            String fda_status,

            @ToolArg(description = "Minimum sensitivity percentage (0-100). " +
                    "Example: 50 for tests with ≥50% sensitivity",
                    required = false)
            Double min_sensitivity,

            @ToolArg(description = "Minimum specificity percentage (0-100). " +
                    "Example: 99 for tests with ≥99% specificity",
                    required = false)
            Double min_specificity,

            @ToolArg(description = "Maximum list price in USD. " +
                    "Example: 1000 for tests costing ≤$1000",
                    required = false)
            Double max_price,

            @ToolArg(description = "Comma-separated list of fields to return. " +
                    "Example: 'id,name,vendor,testScope,sensitivity,listPrice'. " +
                    "Returns all fields if not specified.",
                    required = false)
            String fields,

            @ToolArg(description = "Maximum number of records to return (default: 50, max: 500)",
                    required = false)
            Integer limit,

            @ToolArg(description = "Number of records to skip for pagination (default: 0)",
                    required = false)
            Integer offset
    ) {
        return safeExecute("openonco_search_ecd", () ->
                client.searchEcd(vendor, cancer_type, test_scope, fda_status,
                        min_sensitivity, min_specificity, max_price,
                        fields, limit, offset));
    }

    @Tool(
        title = "openonco_search_hct",
        description = """
            Search and filter HCT (Hereditary Cancer Testing) tests.

            HCT tests identify inherited genetic mutations that increase cancer risk,
            such as BRCA1/BRCA2 for breast/ovarian cancer or Lynch syndrome genes.
            Use this when users ask about genetic risk assessment, inherited cancer syndromes,
            or family history-based testing.

            Tip: Use openonco_get_hct with a test ID for full details including genes covered.
            Filter by cancer_type to find tests covering specific hereditary cancer syndromes.

            Returns JSON array of matching tests. Empty array [] if no matches.
            Default: 50 results, max: 500. Use 'fields' to limit response size.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_search_hct",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_search_hct(
            @ToolArg(description = "Filter by vendor/company name (partial match, case-insensitive). " +
                    "Examples: 'Myriad', 'Invitae', 'Ambry'",
                    required = false)
            String vendor,

            @ToolArg(description = "Filter by cancer type/syndrome (matches within cancerTypes array). " +
                    "Examples: 'Breast', 'Ovarian', 'Colorectal', 'Lynch syndrome'",
                    required = false)
            String cancer_type,

            @ToolArg(description = "Filter by FDA regulatory status (partial match). " +
                    "Examples: 'FDA', 'CLIA', 'approved'",
                    required = false)
            String fda_status,

            @ToolArg(description = "Minimum number of genes analyzed. " +
                    "Example: 30 for tests analyzing ≥30 genes",
                    required = false)
            Integer min_genes,

            @ToolArg(description = "Comma-separated list of fields to return. " +
                    "Example: 'id,name,vendor,genesAnalyzed,cancerTypes'. " +
                    "Returns all fields if not specified.",
                    required = false)
            String fields,

            @ToolArg(description = "Maximum number of records to return (default: 50, max: 500)",
                    required = false)
            Integer limit,

            @ToolArg(description = "Number of records to skip for pagination (default: 0)",
                    required = false)
            Integer offset
    ) {
        return safeExecute("openonco_search_hct", () ->
                client.searchHct(vendor, cancer_type, fda_status, min_genes,
                        fields, limit, offset));
    }

    @Tool(
        title = "openonco_search_tds",
        description = """
            Search and filter TDS (Treatment Decision Support) tests.

            TDS tests guide treatment via comprehensive genomic profiling (CGP), identifying
            actionable mutations for targeted therapies and immunotherapy decisions.
            Use this when users ask about tumor profiling, NGS panels, or companion diagnostics.

            Tip: Filter by has_fda_cdx=true for tests with FDA companion diagnostic approvals.
            Use min_genes to find panels with sufficient gene coverage for specific needs.

            Returns JSON array of matching tests. Empty array [] if no matches.
            Default: 50 results, max: 500. Use 'fields' to limit response size.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_search_tds",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_search_tds(
            @ToolArg(description = "Filter by vendor/company name (partial match, case-insensitive). " +
                    "Examples: 'Foundation', 'Tempus', 'Caris'",
                    required = false)
            String vendor,

            @ToolArg(description = "Filter by cancer type (matches within cancerTypes array). " +
                    "Examples: 'NSCLC', 'Breast', 'Pan-solid'",
                    required = false)
            String cancer_type,

            @ToolArg(description = "Filter by product type. " +
                    "Values: 'Central Lab Service' or 'Laboratory IVD Kit'",
                    required = false)
            String product_type,

            @ToolArg(description = "Filter by sample category. " +
                    "Values: 'Tissue' (solid tumor biopsy) or 'Blood/Plasma' (liquid biopsy)",
                    required = false)
            String sample_category,

            @ToolArg(description = "Filter by testing approach. " +
                    "Values: 'Tissue CGP', 'Liquid CGP', 'Hybrid CGP'",
                    required = false)
            String approach,

            @ToolArg(description = "Filter by FDA regulatory status (partial match). " +
                    "Examples: 'FDA', 'approved', 'cleared'",
                    required = false)
            String fda_status,

            @ToolArg(description = "Minimum number of genes analyzed. " +
                    "Example: 300 for tests analyzing ≥300 genes",
                    required = false)
            Integer min_genes,

            @ToolArg(description = "Filter by FDA companion diagnostic (CDx) approvals. " +
                    "true = has FDA CDx approvals, false = no FDA CDx approvals",
                    required = false)
            Boolean has_fda_cdx,

            @ToolArg(description = "Comma-separated list of fields to return. " +
                    "Example: 'id,name,vendor,genesAnalyzed,fdaCompanionDxCount'. " +
                    "Returns all fields if not specified.",
                    required = false)
            String fields,

            @ToolArg(description = "Maximum number of records to return (default: 50, max: 500)",
                    required = false)
            Integer limit,

            @ToolArg(description = "Number of records to skip for pagination (default: 0)",
                    required = false)
            Integer offset
    ) {
        return safeExecute("openonco_search_tds", () ->
                client.searchTds(vendor, cancer_type, product_type, sample_category,
                        approach, fda_status, min_genes, has_fda_cdx,
                        fields, limit, offset));
    }

    // ========================================
    // PAP (Patient Assistance Programs) TOOLS
    // ========================================

    @Tool(
        title = "openonco_search_pap",
        description = """
            Search vendor patient assistance programs.

            Returns eligibility rules, FPL thresholds, contact info, and application URLs.
            Use when users ask about financial assistance, copay programs, or help paying for tests.

            Tip: Use openonco_get_pap with a program ID or vendor name for full details
            including required documentation and processing time.

            Returns JSON array of matching programs. Empty array [] if no matches.
            Default: 20 results. Use 'fields' to limit response size.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_search_pap",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_search_pap(
            @ToolArg(description = "Filter by vendor name (partial match, case-insensitive). " +
                    "Examples: 'Natera', 'Guardant', 'Foundation'",
                    required = false)
            String vendor,

            @ToolArg(description = "Filter by Medicare eligibility. " +
                    "true = Medicare patients eligible, false = Medicare patients not eligible",
                    required = false)
            Boolean medicare,

            @ToolArg(description = "Filter by Medicaid eligibility. " +
                    "true = Medicaid patients eligible, false = Medicaid patients not eligible",
                    required = false)
            Boolean medicaid,

            @ToolArg(description = "Comma-separated list of fields to return. " +
                    "Example: 'id,vendorName,programName,phone,applicationUrl'. " +
                    "Returns all fields if not specified.",
                    required = false)
            String fields,

            @ToolArg(description = "Maximum number of records to return (default: 20, max: 500)",
                    required = false)
            Integer limit
    ) {
        return safeExecute("openonco_search_pap", () ->
                client.searchPap(vendor, medicare, medicaid, fields, limit, null));
    }

    @Tool(
        title = "openonco_get_pap",
        description = """
            Get complete PAP details for a specific vendor.

            Retrieves ALL fields for one program including eligibility rules, FPL thresholds,
            required documentation, processing time, and contact information.
            Use this after openonco_search_pap to get full details of a specific program.

            Provide either 'id' (e.g., 'pap-1') or 'name' (vendor name like 'Natera').
            Returns JSON object with all fields, or error with NOT_FOUND if program doesn't exist.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_get_pap",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_get_pap(
            @ToolArg(description = "Program ID (e.g., 'pap-1'). Takes precedence if both id and name provided.",
                    required = false)
            String id,

            @ToolArg(description = "Vendor name (exact match, case-insensitive). " +
                    "Example: 'Natera'",
                    required = false)
            String name
    ) {
        return safeExecute("openonco_get_pap", () -> client.getPap(id, name));
    }

    // ========================================
    // GET TOOLS (Detail)
    // ========================================

    @Tool(
        title = "openonco_get_mrd",
        description = """
            Get complete details of a single MRD (Molecular Residual Disease) test.

            Retrieves ALL fields for one test, including sensitivity, specificity, LOD,
            turnaround times, clinical settings, and regulatory status.
            Use this after openonco_search_mrd to get full details of a specific test.

            Provide either 'id' (e.g., 'mrd-1') or 'name' (e.g., 'Signatera').
            Returns JSON object with all fields, or error with NOT_FOUND if test doesn't exist.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_get_mrd",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_get_mrd(
            @ToolArg(description = "Test ID (e.g., 'mrd-1'). Takes precedence if both id and name provided.",
                    required = false)
            String id,

            @ToolArg(description = "Test name (exact match, case-insensitive). " +
                    "Example: 'Signatera'",
                    required = false)
            String name
    ) {
        return safeExecute("openonco_get_mrd", () -> client.getMrd(id, name));
    }

    @Tool(
        title = "openonco_get_ecd",
        description = """
            Get complete details of a single ECD (Early Cancer Detection) test.

            Retrieves ALL fields for one test, including stage-specific sensitivity,
            specificity, PPV, NPV, price, and regulatory status.
            Use this after openonco_search_ecd to get full details of a specific test.

            Provide either 'id' (e.g., 'ecd-1') or 'name' (e.g., 'Galleri').
            Returns JSON object with all fields, or error with NOT_FOUND if test doesn't exist.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_get_ecd",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_get_ecd(
            @ToolArg(description = "Test ID (e.g., 'ecd-1'). Takes precedence if both id and name provided.",
                    required = false)
            String id,

            @ToolArg(description = "Test name (exact match, case-insensitive). " +
                    "Example: 'Galleri'",
                    required = false)
            String name
    ) {
        return safeExecute("openonco_get_ecd", () -> client.getEcd(id, name));
    }

    @Tool(
        title = "openonco_get_hct",
        description = """
            Get complete details of a single HCT (Hereditary Cancer Testing) test.

            Retrieves ALL fields for one test, including genes analyzed, cancer syndromes
            covered, sample requirements, and regulatory status.
            Use this after openonco_search_hct to get full details of a specific test.

            Provide either 'id' (e.g., 'hct-1') or 'name' (e.g., 'myRisk').
            Returns JSON object with all fields, or error with NOT_FOUND if test doesn't exist.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_get_hct",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_get_hct(
            @ToolArg(description = "Test ID (e.g., 'hct-1'). Takes precedence if both id and name provided.",
                    required = false)
            String id,

            @ToolArg(description = "Test name (exact match, case-insensitive). " +
                    "Example: 'myRisk'",
                    required = false)
            String name
    ) {
        return safeExecute("openonco_get_hct", () -> client.getHct(id, name));
    }

    @Tool(
        title = "openonco_get_tds",
        description = """
            Get complete details of a single TDS (Treatment Decision Support) test.

            Retrieves ALL fields for one test, including genes analyzed, biomarkers reported,
            FDA CDx count, turnaround time, price, and NCCN alignment claims.
            Use this after openonco_search_tds to get full details of a specific test.

            Provide either 'id' (e.g., 'tds-1') or 'name' (e.g., 'FoundationOne CDx').
            Returns JSON object with all fields, or error with NOT_FOUND if test doesn't exist.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_get_tds",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_get_tds(
            @ToolArg(description = "Test ID (e.g., 'tds-1'). Takes precedence if both id and name provided.",
                    required = false)
            String id,

            @ToolArg(description = "Test name (exact match, case-insensitive). " +
                    "Example: 'FoundationOne CDx'",
                    required = false)
            String name
    ) {
        return safeExecute("openonco_get_tds", () -> client.getTds(id, name));
    }

    // ========================================
    // COMPARE TOOLS
    // ========================================

    @Tool(
        title = "openonco_compare_mrd",
        description = """
            Compare multiple MRD (Molecular Residual Disease) tests side-by-side.

            Returns key metrics for multiple tests in a tabular comparison format.
            Use this when users want to compare specific MRD tests or need help choosing
            between options (e.g., "Compare Signatera vs Guardant Reveal").

            Tip: Use openonco_search_mrd first to find test IDs, then compare.
            Specify custom 'metrics' to focus on specific attributes.

            Default metrics: name, vendor, sensitivity, specificity, lod, initialTat,
            followUpTat, fdaStatus, reimbursement. Returns JSON array of test objects.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_compare_mrd",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_compare_mrd(
            @ToolArg(description = "Comma-separated test IDs to compare (e.g., 'mrd-1,mrd-2,mrd-3'). " +
                    "Takes precedence if both ids and names provided.",
                    required = false)
            String ids,

            @ToolArg(description = "Comma-separated test names to compare (e.g., 'Signatera,Guardant Reveal'). " +
                    "Names are matched case-insensitively.",
                    required = false)
            String names,

            @ToolArg(description = "Comma-separated metrics to include in comparison. " +
                    "Example: 'name,vendor,sensitivity,specificity,lod'. " +
                    "Uses default metrics if not specified.",
                    required = false)
            String metrics
    ) {
        return safeExecute("openonco_compare_mrd", () -> client.compareMrd(ids, names, metrics));
    }

    @Tool(
        title = "openonco_compare_ecd",
        description = """
            Compare multiple ECD (Early Cancer Detection) tests side-by-side.

            Returns key metrics for multiple tests in a tabular comparison format.
            Use this when users want to compare screening tests or evaluate MCED options
            (e.g., "Compare Galleri vs Shield vs CancerSEEK").

            Tip: Use openonco_search_ecd first to find test IDs, then compare.
            Include 'listPrice' in metrics for cost comparison.

            Default metrics: name, vendor, testScope, sensitivity, specificity,
            stageISensitivity, ppv, npv, listPrice, fdaStatus. Returns JSON array.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_compare_ecd",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_compare_ecd(
            @ToolArg(description = "Comma-separated test IDs to compare (e.g., 'ecd-1,ecd-2,ecd-3'). " +
                    "Takes precedence if both ids and names provided.",
                    required = false)
            String ids,

            @ToolArg(description = "Comma-separated test names to compare (e.g., 'Galleri,Shield'). " +
                    "Names are matched case-insensitively.",
                    required = false)
            String names,

            @ToolArg(description = "Comma-separated metrics to include in comparison. " +
                    "Example: 'name,vendor,testScope,sensitivity,listPrice'. " +
                    "Uses default metrics if not specified.",
                    required = false)
            String metrics
    ) {
        return safeExecute("openonco_compare_ecd", () -> client.compareEcd(ids, names, metrics));
    }

    @Tool(
        title = "openonco_compare_hct",
        description = """
            Compare multiple HCT (Hereditary Cancer Testing) tests side-by-side.

            Returns key metrics for multiple tests in a tabular comparison format.
            Use this when users want to compare hereditary cancer panels or evaluate
            gene coverage across different vendors.

            Tip: Use openonco_search_hct first to find test IDs, then compare.
            Include 'genesAnalyzed' in metrics to compare panel sizes.

            Default metrics: name, vendor, genesAnalyzed, cancerTypes, sampleType,
            tat, listPrice, fdaStatus. Returns JSON array of test objects.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_compare_hct",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_compare_hct(
            @ToolArg(description = "Comma-separated test IDs to compare (e.g., 'hct-1,hct-2,hct-3'). " +
                    "Takes precedence if both ids and names provided.",
                    required = false)
            String ids,

            @ToolArg(description = "Comma-separated test names to compare (e.g., 'myRisk,BRACAnalysis'). " +
                    "Names are matched case-insensitively.",
                    required = false)
            String names,

            @ToolArg(description = "Comma-separated metrics to include in comparison. " +
                    "Example: 'name,vendor,genesAnalyzed,cancerTypes'. " +
                    "Uses default metrics if not specified.",
                    required = false)
            String metrics
    ) {
        return safeExecute("openonco_compare_hct", () -> client.compareHct(ids, names, metrics));
    }

    @Tool(
        title = "openonco_compare_tds",
        description = """
            Compare multiple TDS (Treatment Decision Support) tests side-by-side.

            Returns key metrics for multiple tests in a tabular comparison format.
            Use this when users want to compare CGP panels or evaluate gene coverage
            and FDA companion diagnostic approvals across options.

            Tip: Use openonco_search_tds first to find test IDs, then compare.
            Include 'fdaCompanionDxCount' to see regulatory approval breadth.

            Default metrics: name, vendor, productType, genesAnalyzed,
            fdaCompanionDxCount, tat, listPrice, fdaStatus. Returns JSON array.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_compare_tds",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_compare_tds(
            @ToolArg(description = "Comma-separated test IDs to compare (e.g., 'tds-1,tds-2,tds-3'). " +
                    "Takes precedence if both ids and names provided.",
                    required = false)
            String ids,

            @ToolArg(description = "Comma-separated test names to compare (e.g., 'FoundationOne CDx,Tempus xT'). " +
                    "Names are matched case-insensitively.",
                    required = false)
            String names,

            @ToolArg(description = "Comma-separated metrics to include in comparison. " +
                    "Example: 'name,vendor,genesAnalyzed,fdaCompanionDxCount'. " +
                    "Uses default metrics if not specified.",
                    required = false)
            String metrics
    ) {
        return safeExecute("openonco_compare_tds", () -> client.compareTds(ids, names, metrics));
    }

    // ========================================
    // COUNT TOOLS (Aggregate)
    // ========================================

    @Tool(
        title = "openonco_count_mrd",
        description = """
            Count MRD (Molecular Residual Disease) tests with optional grouping.

            Get aggregate statistics about MRD tests. Use this to answer questions like
            "How many MRD tests are available?" or "Which vendors have the most tests?"

            Without group_by: returns {"total": N}
            With group_by: returns {"total": N, "by_{field}": {"value1": count, ...}}

            Use openonco_list_vendors first to see available vendors for filtering.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_count_mrd",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_count_mrd(
            @ToolArg(description = "Field to group counts by. " +
                    "Valid options: vendor, approach, fdaStatus, requiresTumorTissue, reimbursement. " +
                    "Omit to get only the total count.",
                    required = false)
            String group_by,

            @ToolArg(description = "Filter by vendor name before counting (partial match)",
                    required = false)
            String filter_vendor,

            @ToolArg(description = "Filter by approach before counting. " +
                    "Values: 'Tumor-informed' or 'Tumor-naive'",
                    required = false)
            String filter_approach
    ) {
        return safeExecute("openonco_count_mrd", () -> client.countMrd(group_by, filter_vendor, filter_approach));
    }

    @Tool(
        title = "openonco_count_ecd",
        description = """
            Count ECD (Early Cancer Detection) tests with optional grouping.

            Get aggregate statistics about ECD tests. Use this to answer questions like
            "How many MCED tests exist?" or "How many tests per vendor?"

            Without group_by: returns {"total": N}
            With group_by: returns {"total": N, "by_{field}": {"value1": count, ...}}

            Tip: group_by='testScope' shows Single-cancer vs Multi-cancer breakdown.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_count_ecd",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_count_ecd(
            @ToolArg(description = "Field to group counts by. " +
                    "Valid options: vendor, testScope, fdaStatus, reimbursement. " +
                    "Omit to get only the total count.",
                    required = false)
            String group_by,

            @ToolArg(description = "Filter by vendor name before counting (partial match)",
                    required = false)
            String filter_vendor,

            @ToolArg(description = "Filter by test scope before counting. " +
                    "Values: 'Single-cancer' or 'Multi-cancer'",
                    required = false)
            String filter_test_scope
    ) {
        return safeExecute("openonco_count_ecd", () -> client.countEcd(group_by, filter_vendor, filter_test_scope));
    }

    @Tool(
        title = "openonco_count_hct",
        description = """
            Count HCT (Hereditary Cancer Testing) tests with optional grouping.

            Get aggregate statistics about HCT tests. Use this to answer questions like
            "How many hereditary cancer tests are available?" or "How many tests per vendor?"

            Without group_by: returns {"total": N}
            With group_by: returns {"total": N, "by_{field}": {"value1": count, ...}}

            Tip: group_by='vendor' shows distribution across testing companies.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_count_hct",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_count_hct(
            @ToolArg(description = "Field to group counts by. " +
                    "Valid options: vendor, fdaStatus, reimbursement, sampleCategory. " +
                    "Omit to get only the total count.",
                    required = false)
            String group_by,

            @ToolArg(description = "Filter by vendor name before counting (partial match)",
                    required = false)
            String filter_vendor,

            @ToolArg(description = "Filter by FDA status before counting (partial match)",
                    required = false)
            String filter_fda_status
    ) {
        return safeExecute("openonco_count_hct", () ->
                client.countHct(group_by, filter_vendor, filter_fda_status));
    }

    @Tool(
        title = "openonco_count_tds",
        description = """
            Count TDS (Treatment Decision Support) tests with optional grouping.

            Get aggregate statistics about TDS tests. Use this to answer questions like
            "How many CGP panels exist?" or "How many lab services vs IVD kits?"

            Without group_by: returns {"total": N}
            With group_by: returns {"total": N, "by_{field}": {"value1": count, ...}}

            Tip: group_by='productType' shows Central Lab Service vs Laboratory IVD Kit.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_count_tds",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_count_tds(
            @ToolArg(description = "Field to group counts by. " +
                    "Valid options: vendor, productType, fdaStatus, approach, reimbursement. " +
                    "Omit to get only the total count.",
                    required = false)
            String group_by,

            @ToolArg(description = "Filter by vendor name before counting (partial match)",
                    required = false)
            String filter_vendor,

            @ToolArg(description = "Filter by product type before counting. " +
                    "Values: 'Central Lab Service' or 'Laboratory IVD Kit'",
                    required = false)
            String filter_product_type
    ) {
        return safeExecute("openonco_count_tds", () -> client.countTds(group_by, filter_vendor, filter_product_type));
    }

    // ========================================
    // LIST TOOLS
    // ========================================

    @Tool(
        title = "openonco_list_vendors",
        description = """
            List all vendors offering oncology diagnostic tests.

            Use this to discover available vendors before searching or filtering.
            Helpful for answering "What companies offer MRD tests?" or building
            autocomplete suggestions for vendor names.

            Returns JSON array of vendor names sorted alphabetically.
            Filter by category to see vendors in specific test types only.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_list_vendors",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_list_vendors(
            @ToolArg(description = "Filter by test category. " +
                    "Values: 'mrd', 'ecd', 'hct', 'tds'. " +
                    "Omit to list vendors across all categories.",
                    required = false)
            String category
    ) {
        return safeExecute("openonco_list_vendors", () -> client.listVendors(category));
    }

    @Tool(
        title = "openonco_list_cancer_types",
        description = """
            List all cancer types covered by oncology diagnostic tests.

            Use this to discover available cancer types before searching or filtering.
            Helpful for answering "What cancers are covered?" or building filter options.

            Returns JSON array of cancer type names sorted alphabetically.
            Filter by category to see cancer types in specific test types only.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_list_cancer_types",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_list_cancer_types(
            @ToolArg(description = "Filter by test category. " +
                    "Values: 'mrd', 'ecd', 'hct', 'tds'. " +
                    "Omit to list cancer types across all categories.",
                    required = false)
            String category
    ) {
        return safeExecute("openonco_list_cancer_types", () -> client.listCancerTypes(category));
    }

    @Tool(
        title = "openonco_list_categories",
        description = """
            List all test categories with metadata and current test counts.

            Use this FIRST to understand what data is available. Returns the four
            oncology diagnostic test categories: MRD, ECD, HCT, TDS.

            Each category includes: id, name, shortName, description, testCount.
            The testCount reflects the actual number of tests currently in each category.

            This is a good starting point for users new to the OpenOnco data.
            """,
        annotations = @Tool.Annotations(
                    title = "openonco_list_categories",
                    readOnlyHint = true,
                    destructiveHint = false,
                    idempotentHint = true,
                    openWorldHint = false))
    public ToolResponse openonco_list_categories() {
        return safeExecute("openonco_list_categories", () -> client.listCategories());
    }
}
