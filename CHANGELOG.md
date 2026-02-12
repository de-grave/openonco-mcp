# Changelog

All notable changes to the OpenOnco MCP Server are documented here.

## [Unreleased]

### Added
- `.gitignore` file for build artifacts, IDE files, and OS files
- PAP test coverage in `SearchToolsTest` and `DetailCompareToolsTest`
- `pap_programs` table validation in `DuckDbServiceTest`
- `CHANGELOG.md` for tracking changes

### Changed
- Updated README to reflect 21 tools (was 19), 5 categories (was 4)
- Replaced all TRM references with HCT in README documentation
- Added PAP tools and data model documentation to README

### Removed
- `target/` directory from git tracking (89MB of build artifacts)

## [1.0.3] - 2026-02-07

### Added
- Favicon for MCP Directory listing
- MCP Directory submission requirements

## [1.0.2] - 2026-02-05

### Added
- PAP (Patient Assistance Programs) category with `search_pap` and `get_pap` tools
- `pap.json` data file with vendor financial assistance programs
- Medicare/Medicaid eligibility filtering for PAP programs

### Changed
- Synced MCP data from OpenOnco main database

## [1.0.1] - 2026-02-03

### Changed
- Replaced TRM (Treatment Response Monitoring) category with HCT (Hereditary Cancer Testing)
- Added `hct.json` data file with 33 hereditary cancer tests
- Added `search_hct`, `get_hct`, `compare_hct`, `count_hct` tools
- Clarified MCP endpoints: recommend Streamable HTTP over legacy SSE
- Added `quarkus-smallrye-health` for `/q/health` endpoint
- Extended healthcheck timeout to 120s for Railway deployment
- Skip validation for empty tables (TRM had 0 records during transition)

### Fixed
- Build uber-jar explicitly with exact filename for Railway
- Use Dockerfile with explicit Java 21 instead of Nixpacks
- Fixed runner jar path wildcard

## [1.0.0] - 2026-01-28

### Added
- Initial release with 19 MCP tools
- Four test categories: MRD, ECD, TRM, TDS
- Search, Get, Compare, Count, and List tools
- DuckDB in-memory database for fast queries
- Quarkus MCP Server with SSE and stdio transport
- Railway deployment configuration
- Comprehensive test suite (SearchToolsTest, AggregateListToolsTest, DetailCompareToolsTest, DuckDbServiceTest, QueryBuilderTest)
