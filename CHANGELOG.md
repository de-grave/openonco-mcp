# Changelog

All notable changes to this project made by Claude will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed - 2026-02-01
#### Correct Error handling with isError=true in response

  Changes Made

  - `safeExecute` now either returns a proper ToolResponse on success, or Throws ToolCallException on failure
    so Quarkus sets isError=true in response
  - class `McpResponse` added
  - `title` annotations added to all tools

### Changed - 2026-01-31
#### Fix HCT cancerTypesAssessed Field Name Mismatch

  HCT data uses `cancerTypesAssessed` but implementation code referenced `cancerTypes`, causing 4 test failures.

  Changes Made

  - Added `cancerTypesAssessed` to `ARRAY_FIELDS` in `QueryBuilder.java` for proper array handling
  - Updated `DEFAULT_HCT_METRICS` in `OpenOncoClient.java` to use `cancerTypesAssessed`
  - Fixed `searchHct()` filter mapping to use `cancerTypesAssessed` instead of `cancerTypes`
  - Rewrote `listCancerTypes()` to handle HCT's different field name vs other categories
  - Updated `testCompareHct_WithCustomMetrics` test to use correct field name

