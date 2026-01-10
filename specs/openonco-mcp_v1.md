# OpenOnco MCP Server Specification v1.0

## Overview

OpenOnco MCP Server is a Quarkus-based Model Context Protocol (MCP) server that provides AI assistants with tools to query oncology diagnostic test data. The server loads JSON data files into an in-memory DuckDB database at startup and exposes category-specific MCP tools for searching, filtering, comparing, and analyzing tests.

### Domain Context

OpenOnco covers four categories of oncology diagnostic tests:

| Category | Full Name | Purpose |
|----------|-----------|---------|
| **MRD** | Molecular Residual Disease | Detect residual cancer after treatment via ctDNA |
| **ECD** | Early Cancer Detection | Screen for cancer in asymptomatic individuals |
| **TRM** | Treatment Response Monitoring | Track treatment effectiveness via ctDNA changes |
| **TDS** | Treatment Decision Support | Guide treatment decisions via genomic profiling |

---

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    MCP Clients (Claude, etc.)               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   OpenOncoMCPServer                         │
│  @ApplicationScoped bean with @Tool annotated methods       │
│  - Category-specific search tools                           │
│  - Detail lookup tools                                      │
│  - Comparison tools                                         │
│  - Aggregation tools                                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     OpenOncoClient                          │
│  - Delegates queries to DuckDbService                       │
│  - Transforms results to JSON responses                     │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     DuckDbService                           │
│  @ApplicationScoped bean                                    │
│  - Initializes in-memory DuckDB on startup                  │
│  - Loads JSON files into tables                             │
│  - Executes parameterized SQL queries                       │
│  - Manages connection lifecycle                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    DuckDB (In-Memory)                       │
│  Tables: mrd_tests, ecd_tests, trm_tests, tds_tests         │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **Startup**: `DuckDbService` loads JSON files from `src/main/resources/` into DuckDB tables
2. **Request**: MCP client calls a tool (e.g., `openonco_search_mrd`)
3. **Processing**: `OpenOncoMCPServer` delegates to `OpenOncoClient`
4. **Query**: `OpenOncoClient` builds SQL and calls `DuckDbService`
5. **Response**: Results returned as JSON through MCP protocol

---

## Data Model

### Source Files

| File | Table | Description |
|------|-------|-------------|
| `mrd.json` | `mrd_tests` | MRD test records |
| `ecd.json` | `ecd_tests` | ECD test records |
| `trm.json` | `trm_tests` | TRM test records |
| `tds.json` | `tds_tests` | TDS test records |

### Common Fields (All Categories)

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier (e.g., "mrd-1", "ecd-2") |
| `name` | string | Test name (e.g., "Signatera", "Shield") |
| `vendor` | string | Company name |
| `sampleCategory` | string | "Blood/Plasma" or "Tissue" |
| `approach` | string | Testing approach methodology |
| `method` | string | Technical method description |
| `cancerTypes` | string[] | Targeted cancer types |
| `sensitivity` | number | Reported sensitivity (%) |
| `specificity` | number | Reported specificity (%) |
| `fdaStatus` | string | Regulatory status |
| `reimbursement` | string | Coverage status |
| `numPublications` | number | Publication count |
| `totalParticipants` | number | Clinical trial participants |

### Category-Specific Fields

#### MRD Tests
- `requiresTumorTissue`: Yes/No
- `requiresMatchedNormal`: Yes/No
- `variantsTracked`: Number of variants
- `lod`, `lod95`: Limit of detection
- `initialTat`, `followUpTat`: Turnaround times
- `clinicalSettings`: ["Post-Surgery", "Surveillance"]
- `stageIISensitivity`, `stageIIISensitivity`: Stage-specific sensitivity

#### ECD Tests
- `testScope`: "Single-cancer (CRC)" or "Multi-cancer (MCED)"
- `targetPopulation`: Target screening population
- `stageISensitivity` through `stageIVSensitivity`: By-stage sensitivity
- `ppv`, `npv`: Predictive values
- `screeningInterval`: Recommended interval
- `listPrice`: Test price

#### TRM Tests
- `responseDefinition`: How response is defined
- `leadTimeVsImaging`: Days earlier than imaging
- `isDiscontinued`: Product status

#### TDS Tests
- `productType`: "Central Lab Service" or "Laboratory IVD Kit"
- `genesAnalyzed`: Number of genes
- `biomarkersReported`: ["SNVs", "Indels", "CNAs", ...]
- `fdaCompanionDxCount`: FDA companion diagnostic indications
- `vendorClaimsNCCNAlignment`: NCCN guideline alignment

---

## MCP Tools Specification

### Naming Convention

All tools use prefix `openonco_` followed by `{action}_{category}` pattern:
- `openonco_search_mrd` - Search MRD tests
- `openonco_get_ecd` - Get single ECD test
- `openonco_compare_tds` - Compare TDS tests
- `openonco_count_trm` - Count TRM tests

### Tool Categories

1. **Search Tools** - Filter and search tests by criteria
2. **Detail Tools** - Get complete details of a single test
3. **Compare Tools** - Compare multiple tests by specific metrics
4. **Aggregate Tools** - Count and group tests by attributes
5. **List Tools** - Get distinct values for filtering

---

## Tool Definitions

### 1. Search Tools

#### `openonco_search_mrd`

Search and filter MRD (Molecular Residual Disease) tests.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `vendor` | string | No | Filter by vendor name (partial match, case-insensitive) |
| `cancer_type` | string | No | Filter by cancer type (partial match in cancerTypes array) |
| `approach` | string | No | Filter by approach: "Tumor-informed" or "Tumor-naive" |
| `fda_status` | string | No | Filter by FDA status (partial match) |
| `min_sensitivity` | number | No | Minimum sensitivity percentage (0-100) |
| `requires_tumor_tissue` | boolean | No | Filter by tumor tissue requirement |
| `clinical_setting` | string | No | Filter by clinical setting: "Post-Surgery" or "Surveillance" |
| `fields` | string | No | Comma-separated list of fields to return (default: all) |
| `limit` | integer | No | Maximum records to return (default: 50) |
| `offset` | integer | No | Number of records to skip for pagination (default: 0) |

**Returns:** JSON array of matching MRD test records with requested fields.

**Example:**
```json
{
  "vendor": "Natera",
  "approach": "Tumor-informed",
  "min_sensitivity": 90,
  "limit": 10
}
```

---

#### `openonco_search_ecd`

Search and filter ECD (Early Cancer Detection) tests.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `vendor` | string | No | Filter by vendor name |
| `cancer_type` | string | No | Filter by cancer type |
| `test_scope` | string | No | "Single-cancer" or "Multi-cancer" |
| `fda_status` | string | No | Filter by FDA status |
| `min_sensitivity` | number | No | Minimum overall sensitivity |
| `min_specificity` | number | No | Minimum specificity |
| `max_price` | number | No | Maximum list price |
| `fields` | string | No | Fields to return |
| `limit` | integer | No | Max records (default: 50) |
| `offset` | integer | No | Pagination offset |

---

#### `openonco_search_trm`

Search and filter TRM (Treatment Response Monitoring) tests.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `vendor` | string | No | Filter by vendor name |
| `cancer_type` | string | No | Filter by cancer type |
| `approach` | string | No | "Tumor-informed" or "Tumor-agnostic" |
| `fda_status` | string | No | Filter by FDA status |
| `include_discontinued` | boolean | No | Include discontinued tests (default: false) |
| `fields` | string | No | Fields to return |
| `limit` | integer | No | Max records (default: 50) |
| `offset` | integer | No | Pagination offset |

---

#### `openonco_search_tds`

Search and filter TDS (Treatment Decision Support) tests.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `vendor` | string | No | Filter by vendor name |
| `cancer_type` | string | No | Filter by cancer type |
| `product_type` | string | No | "Central Lab Service" or "Laboratory IVD Kit" |
| `sample_category` | string | No | "Tissue" or "Blood/Plasma" |
| `approach` | string | No | "Tissue CGP", "Liquid CGP", etc. |
| `fda_status` | string | No | Filter by FDA status |
| `min_genes` | integer | No | Minimum genes analyzed |
| `has_fda_cdx` | boolean | No | Has FDA companion diagnostic approvals |
| `fields` | string | No | Fields to return |
| `limit` | integer | No | Max records (default: 50) |
| `offset` | integer | No | Pagination offset |

---

### 2. Detail Tools

#### `openonco_get_mrd`

Get complete details of a single MRD test by ID or name.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | No* | Test ID (e.g., "mrd-1") |
| `name` | string | No* | Test name (e.g., "Signatera") |

*One of `id` or `name` is required.

**Returns:** Complete JSON object with all fields for the test.

---

#### `openonco_get_ecd`

Get complete details of a single ECD test.

**Parameters:** Same as `openonco_get_mrd`

---

#### `openonco_get_trm`

Get complete details of a single TRM test.

**Parameters:** Same as `openonco_get_mrd`

---

#### `openonco_get_tds`

Get complete details of a single TDS test.

**Parameters:** Same as `openonco_get_mrd`

---

### 3. Compare Tools

#### `openonco_compare_mrd`

Compare multiple MRD tests side-by-side on specified metrics.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `ids` | string | No* | Comma-separated test IDs |
| `names` | string | No* | Comma-separated test names |
| `metrics` | string | No | Comma-separated metrics to compare (default: key metrics) |

*One of `ids` or `names` is required.

**Default Metrics:** `name, vendor, sensitivity, specificity, lod, initialTat, followUpTat, fdaStatus, reimbursement`

**Returns:** JSON array with each test showing only the comparison metrics.

---

#### `openonco_compare_ecd`

Compare multiple ECD tests.

**Default Metrics:** `name, vendor, testScope, sensitivity, specificity, stageISensitivity, ppv, npv, listPrice, fdaStatus`

---

#### `openonco_compare_trm`

Compare multiple TRM tests.

**Default Metrics:** `name, vendor, approach, sensitivity, leadTimeVsImaging, lod, fdaStatus, reimbursement`

---

#### `openonco_compare_tds`

Compare multiple TDS tests.

**Default Metrics:** `name, vendor, productType, genesAnalyzed, fdaCompanionDxCount, tat, listPrice, fdaStatus`

---

### 4. Aggregate Tools

#### `openonco_count_mrd`

Get counts of MRD tests grouped by an attribute.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `group_by` | string | No | Field to group by (default: returns total count) |
| `filter_vendor` | string | No | Filter by vendor before counting |
| `filter_approach` | string | No | Filter by approach before counting |

**Example Response:**
```json
{
  "total": 12,
  "by_vendor": {
    "Natera": 3,
    "Guardant Health": 2,
    "Foundation Medicine": 1
  }
}
```

---

#### `openonco_count_ecd`, `openonco_count_trm`, `openonco_count_tds`

Similar structure with category-appropriate filters.

---

### 5. List Tools

#### `openonco_list_vendors`

Get list of all vendors across all categories or specific category.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `category` | string | No | Filter by category: "mrd", "ecd", "trm", "tds" (default: all) |

**Returns:** JSON array of distinct vendor names.

---

#### `openonco_list_cancer_types`

Get list of all cancer types across all categories or specific category.

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `category` | string | No | Filter by category (default: all) |

**Returns:** JSON array of distinct cancer types.

---

#### `openonco_list_categories`

Get metadata about all test categories.

**Parameters:** None

**Returns:**
```json
[
  {
    "id": "mrd",
    "name": "Molecular Residual Disease",
    "shortName": "MRD Testing",
    "description": "...",
    "testCount": 12
  },
  ...
]
```

---

## Implementation Details

### New Files to Create

#### 1. `src/main/java/org/openonco/db/DuckDbService.java`

```java
package org.openonco.db;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.duckdb.DuckDBConnection;

import java.sql.*;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DuckDbService {

    private Connection connection;

    @PostConstruct
    void init() {
        // Initialize in-memory DuckDB
        // Load JSON files into tables
    }

    @PreDestroy
    void cleanup() {
        // Close connection
    }

    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        // Execute parameterized query and return results
    }

    public int executeCount(String sql, Object... params) {
        // Execute count query
    }
}
```

#### 2. `src/main/java/org/openonco/db/QueryBuilder.java`

```java
package org.openonco.db;

/**
 * Builds parameterized SQL queries from filter criteria.
 * Prevents SQL injection by using prepared statements.
 */
public class QueryBuilder {

    public static QueryResult buildSearchQuery(
        String table,
        Map<String, Object> filters,
        List<String> fields,
        int limit,
        int offset
    ) {
        // Build SELECT with WHERE clauses
        // Return SQL string and parameter values
    }
}
```

### Files to Modify

#### 1. `pom.xml` - Add DuckDB Dependency

```xml
<dependency>
    <groupId>org.duckdb</groupId>
    <artifactId>duckdb_jdbc</artifactId>
    <version>1.1.3</version>
</dependency>
```

#### 2. `OpenOncoMCPServer.java` - Add MCP Tools

Add all tool methods with `@Tool` and `@ToolArg` annotations.

#### 3. `OpenOncoClient.java` - Add Query Methods

Add methods that delegate to `DuckDbService` for each tool.

---

## DuckDB Schema

### Table Creation (executed at startup)

```sql
-- MRD Tests
CREATE TABLE mrd_tests AS
SELECT * FROM read_json_auto('classpath:mrd.json');

-- ECD Tests
CREATE TABLE ecd_tests AS
SELECT * FROM read_json_auto('classpath:ecd.json');

-- TRM Tests
CREATE TABLE trm_tests AS
SELECT * FROM read_json_auto('classpath:trm.json');

-- TDS Tests
CREATE TABLE tds_tests AS
SELECT * FROM read_json_auto('classpath:tds.json');
```

### Query Examples

#### Search with Filters
```sql
SELECT id, name, vendor, sensitivity, specificity, fdaStatus
FROM mrd_tests
WHERE vendor ILIKE '%natera%'
  AND sensitivity >= 90
  AND list_contains(cancerTypes, 'Colorectal')
ORDER BY sensitivity DESC
LIMIT 10 OFFSET 0;
```

#### Get by ID
```sql
SELECT * FROM ecd_tests WHERE id = 'ecd-1';
```

#### Get by Name
```sql
SELECT * FROM tds_tests WHERE name ILIKE '%foundationone%';
```

#### Count by Group
```sql
SELECT vendor, COUNT(*) as count
FROM mrd_tests
GROUP BY vendor
ORDER BY count DESC;
```

#### Distinct Values
```sql
SELECT DISTINCT vendor FROM mrd_tests ORDER BY vendor;
```

#### Array Field Search (DuckDB specific)
```sql
-- Check if array contains value
SELECT * FROM mrd_tests
WHERE list_contains(cancerTypes, 'Breast');

-- Unnest and search
SELECT DISTINCT unnest(cancerTypes) as cancer_type
FROM mrd_tests
ORDER BY cancer_type;
```

---

## Error Handling

### Error Response Format

```json
{
  "error": true,
  "code": "NOT_FOUND",
  "message": "No test found with id 'mrd-999'",
  "suggestion": "Use openonco_search_mrd to find available tests"
}
```

### Error Codes

| Code | Description |
|------|-------------|
| `NOT_FOUND` | Requested resource not found |
| `INVALID_PARAMETER` | Invalid parameter value |
| `MISSING_PARAMETER` | Required parameter missing |
| `QUERY_ERROR` | Database query failed |

---

## Security Considerations

1. **SQL Injection Prevention**: All queries use parameterized statements via `QueryBuilder`
2. **Input Validation**: All parameters validated before query construction
3. **Read-Only**: No data modification operations exposed
4. **No Sensitive Data**: Dataset contains publicly available test information

---

## Testing Strategy

### Unit Tests

1. `DuckDbServiceTest` - Test JSON loading and query execution
2. `QueryBuilderTest` - Test SQL generation with various filters
3. `OpenOncoClientTest` - Test client methods with mocked DuckDbService

### Integration Tests

1. `OpenOncoMCPServerIT` - Test MCP tools end-to-end
2. `SearchToolsIT` - Test all search tool combinations
3. `CompareToolsIT` - Test comparison functionality

### Test Data

Use subset of real data files or create `test-mrd.json`, etc. with representative samples.

---

## Implementation Phases

### Phase 1: Core Infrastructure
1. Add DuckDB dependency to `pom.xml`
2. Implement `DuckDbService` with JSON loading
3. Implement `QueryBuilder` for safe SQL generation
4. Basic connectivity tests

### Phase 2: Search Tools
1. Implement `openonco_search_mrd` (template for others)
2. Implement remaining search tools (ecd, trm, tds)
3. Add pagination support
4. Search tool tests

### Phase 3: Detail & Compare Tools
1. Implement get tools (get_mrd, get_ecd, etc.)
2. Implement compare tools
3. Tests for detail and compare functionality

### Phase 4: Aggregate & List Tools
1. Implement count tools
2. Implement list tools (vendors, cancer_types, categories)
3. Complete test coverage

### Phase 5: Polish & Documentation
1. Error handling refinement
2. Tool description enhancement
3. Example responses in tool descriptions
4. Performance testing with full dataset

---

## Appendix: Full Tool Summary

| Tool | Category | Purpose |
|------|----------|---------|
| `openonco_search_mrd` | Search | Search MRD tests with filters |
| `openonco_search_ecd` | Search | Search ECD tests with filters |
| `openonco_search_trm` | Search | Search TRM tests with filters |
| `openonco_search_tds` | Search | Search TDS tests with filters |
| `openonco_get_mrd` | Detail | Get single MRD test details |
| `openonco_get_ecd` | Detail | Get single ECD test details |
| `openonco_get_trm` | Detail | Get single TRM test details |
| `openonco_get_tds` | Detail | Get single TDS test details |
| `openonco_compare_mrd` | Compare | Compare MRD tests |
| `openonco_compare_ecd` | Compare | Compare ECD tests |
| `openonco_compare_trm` | Compare | Compare TRM tests |
| `openonco_compare_tds` | Compare | Compare TDS tests |
| `openonco_count_mrd` | Aggregate | Count MRD tests |
| `openonco_count_ecd` | Aggregate | Count ECD tests |
| `openonco_count_trm` | Aggregate | Count TRM tests |
| `openonco_count_tds` | Aggregate | Count TDS tests |
| `openonco_list_vendors` | List | List all vendors |
| `openonco_list_cancer_types` | List | List all cancer types |
| `openonco_list_categories` | List | List category metadata |

**Total: 19 MCP Tools**
