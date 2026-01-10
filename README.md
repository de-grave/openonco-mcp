# OpenOnco MCP Server

A Model Context Protocol (MCP) server that provides AI assistants with tools to query oncology diagnostic test data. Built with Quarkus and DuckDB.

## Overview

OpenOnco MCP Server exposes 19 tools for searching, filtering, comparing, and analyzing oncology diagnostic tests across four categories:

| Category | Full Name | Purpose |
|----------|-----------|---------|
| **MRD** | Molecular Residual Disease | Detect residual cancer after treatment via ctDNA |
| **ECD** | Early Cancer Detection | Screen for cancer in asymptomatic individuals |
| **TRM** | Treatment Response Monitoring | Track treatment effectiveness via ctDNA changes |
| **TDS** | Treatment Decision Support | Guide treatment decisions via genomic profiling |

## Quick Start

### Prerequisites

- Java 21 or later
- Maven 3.8+ (or use included `./mvnw`)

### Build

```bash
# Compile
./mvnw compile

# Run tests
./mvnw test

# Build uber-jar
./mvnw package -DskipTests -Dquarkus.package.jar.type=uber-jar
```

### Run

```bash
java -jar target/openonco-mcp-runner.jar
```

## MCP Client Configuration

### Claude Desktop

Add to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "openonco": {
      "command": "java",
      "args": ["-jar", "/path/to/openonco-mcp-runner.jar"]
    }
  }
}
```

### SSE Mode (HTTP)

For HTTP-based MCP clients, the server runs on port 9001:

- **Streamable endpoint**: `http://localhost:9001/mcp`
- **SSE endpoint**: `http://localhost:9001/mcp/sse`

## Tool Reference

### Search Tools

Search tools find tests matching criteria. All return JSON arrays.

#### `openonco_search_mrd`
Search MRD tests by vendor, cancer type, approach, sensitivity, etc.

```
vendor: "Natera"              # Partial match, case-insensitive
cancer_type: "Colorectal"     # Matches in cancerTypes array
approach: "Tumor-informed"    # Or "Tumor-naive"
min_sensitivity: 90           # Minimum sensitivity %
requires_tumor_tissue: true   # Boolean filter
fields: "id,name,vendor"      # Limit returned fields
limit: 10                     # Max results (default: 50)
```

#### `openonco_search_ecd`
Search ECD tests by vendor, test scope, price, etc.

```
test_scope: "Multi-cancer"    # Or "Single-cancer"
max_price: 1000               # Maximum list price
min_specificity: 99           # Minimum specificity %
```

#### `openonco_search_trm`
Search TRM tests. Excludes discontinued by default.

```
include_discontinued: true    # Include legacy products
```

#### `openonco_search_tds`
Search TDS tests by genes, product type, FDA status.

```
min_genes: 300                # Minimum genes analyzed
has_fda_cdx: true             # Has FDA companion diagnostics
product_type: "Central Lab Service"
```

### Get Tools

Retrieve complete details for a single test. Returns JSON object.

#### `openonco_get_mrd`, `openonco_get_ecd`, `openonco_get_trm`, `openonco_get_tds`

```
id: "mrd-1"                   # By ID (takes precedence)
name: "Signatera"             # Or by name (case-insensitive)
```

### Compare Tools

Compare multiple tests side-by-side. Returns JSON array with selected metrics.

#### `openonco_compare_mrd`, `openonco_compare_ecd`, `openonco_compare_trm`, `openonco_compare_tds`

```
ids: "mrd-1,mrd-2,mrd-3"      # Comma-separated IDs
names: "Signatera,Guardant"   # Or comma-separated names
metrics: "name,vendor,sensitivity"  # Custom metrics (optional)
```

**Default metrics per category:**
- MRD: name, vendor, sensitivity, specificity, lod, initialTat, followUpTat, fdaStatus, reimbursement
- ECD: name, vendor, testScope, sensitivity, specificity, stageISensitivity, ppv, npv, listPrice, fdaStatus
- TRM: name, vendor, approach, sensitivity, leadTimeVsImaging, lod, fdaStatus, reimbursement
- TDS: name, vendor, productType, genesAnalyzed, fdaCompanionDxCount, tat, listPrice, fdaStatus

### Count Tools

Get aggregate statistics. Returns JSON object with total and optional grouped counts.

#### `openonco_count_mrd`, `openonco_count_ecd`, `openonco_count_trm`, `openonco_count_tds`

```
group_by: "vendor"            # Group counts by field
filter_vendor: "Natera"       # Pre-filter before counting
```

**Response format:**
```json
{
  "total": 27,
  "by_vendor": {
    "Natera": 3,
    "Guardant Health": 2,
    "Foundation Medicine": 1
  }
}
```

**Valid group_by fields:**
- MRD: vendor, approach, fdaStatus, requiresTumorTissue, reimbursement
- ECD: vendor, testScope, fdaStatus, reimbursement
- TRM: vendor, approach, fdaStatus, reimbursement, isDiscontinued
- TDS: vendor, productType, fdaStatus, approach, reimbursement

### List Tools

Discover available filter values.

#### `openonco_list_vendors`
List all vendor names. Filter by category optionally.

```
category: "mrd"               # Optional: mrd, ecd, trm, tds
```

#### `openonco_list_cancer_types`
List all cancer types. Filter by category optionally.

#### `openonco_list_categories`
Get metadata about all four test categories including current test counts.

**Response:**
```json
[
  {
    "id": "mrd",
    "name": "Molecular Residual Disease",
    "shortName": "MRD Testing",
    "description": "Detect residual cancer after treatment via ctDNA",
    "testCount": 27
  },
  ...
]
```

## Data Model

### Common Fields (All Categories)

| Field | Type | Description |
|-------|------|-------------|
| id | string | Unique identifier (e.g., "mrd-1") |
| name | string | Test name |
| vendor | string | Company name |
| cancerTypes | string[] | Targeted cancer types |
| sensitivity | number | Reported sensitivity (%) |
| specificity | number | Reported specificity (%) |
| fdaStatus | string | Regulatory status |
| reimbursement | string | Coverage status |

### MRD-Specific Fields

| Field | Description |
|-------|-------------|
| approach | "Tumor-informed" or "Tumor-naive" |
| requiresTumorTissue | "Yes" or "No" |
| lod, lod95 | Limit of detection |
| initialTat, followUpTat | Turnaround times |
| clinicalSettings | ["Post-Surgery", "Surveillance"] |

### ECD-Specific Fields

| Field | Description |
|-------|-------------|
| testScope | "Single-cancer" or "Multi-cancer (MCED)" |
| stageISensitivity - stageIVSensitivity | By-stage sensitivity |
| ppv, npv | Predictive values |
| listPrice | Test price in USD |

### TRM-Specific Fields

| Field | Description |
|-------|-------------|
| leadTimeVsImaging | Days earlier than imaging |
| isDiscontinued | Product status (boolean) |

### TDS-Specific Fields

| Field | Description |
|-------|-------------|
| productType | "Central Lab Service" or "Laboratory IVD Kit" |
| genesAnalyzed | Number of genes |
| fdaCompanionDxCount | FDA companion diagnostic indications |
| biomarkersReported | ["SNVs", "Indels", "CNAs", ...] |

## Error Handling

All tools return structured JSON errors:

```json
{
  "error": true,
  "code": "NOT_FOUND",
  "message": "No MRD test found with id 'mrd-999'",
  "suggestion": "Use openonco_search_mrd to find available tests"
}
```

### Error Codes

| Code | Description |
|------|-------------|
| `NOT_FOUND` | Requested test not found |
| `MISSING_PARAMETER` | Required parameter not provided |
| `INVALID_PARAMETER` | Invalid value for parameter |
| `INTERNAL_ERROR` | Unexpected server error |

## Architecture

```
MCP Client (Claude, etc.)
        |
        v
OpenOncoMCPServer (19 @Tool methods)
        |
        v
OpenOncoClient (business logic)
        |
        v
DuckDbService (in-memory DuckDB)
        |
        v
JSON Data Files (mrd.json, ecd.json, trm.json, tds.json)
```

## Development

### Project Structure

```
src/main/java/org/openonco/
  mcp/OpenOncoMCPServer.java     # MCP tool definitions
  client/OpenOncoClient.java     # Business logic
  db/DuckDbService.java          # Database management
  db/QueryBuilder.java           # SQL query construction
  db/OpenOncoException.java      # Custom exceptions

src/main/resources/
  mrd.json, ecd.json, trm.json, tds.json  # Data files
```

### Running Tests

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=SearchToolsTest

# Single test method
./mvnw test -Dtest=SearchToolsTest#testSearchMrd_NoFilters
```

## License

Copyright 2026 OpenOnco. Licensed under Apache License 2.0.
