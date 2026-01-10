# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./mvnw compile              # Compile the project
./mvnw test                 # Run unit tests
./mvnw verify               # Run all tests including integration tests
./mvnw quarkus:dev          # Run in dev mode with live reload
./mvnw package              # Build JAR package
./mvnw package -Dnative     # Build native executable (requires GraalVM)
./mvnw package -DskipTests -Dquarkus.package.jar.type=uber-jar  # Build a single uber-jar 
```

Run a single test:
```bash
./mvnw test -Dtest=ClassName#methodName
```

## Architecture

This is a Quarkus-based MCP (Model Context Protocol) server that exposes tools for AI assistants to query OpenOnco project data.

**Key components:**

- `OpenOncoMCPServer` - MCP server bean that defines tools using `@Tool` annotations. Tools are automatically exposed via MCP protocol.
- `OpenOncoClient` - Client for communicating with OpenOnco backend services.
- `SharedApplication` - Main entry point (`@QuarkusMain`) that bootstraps Quarkus.
- `McpCliConfigSource` - Custom MicroProfile ConfigSource that parses CLI arguments (`--key=value` or `-Dkey=value`) into Quarkus configuration.

**Transport modes:**
- **stdio** (default for dev): Direct stdin/stdout communication with MCP client
- **SSE** (default for prod): HTTP Server-Sent Events on port 9001 at `/mcp`

Toggle with `--sse=true/false` CLI argument or profile-based config in `application.properties`.

**Adding new MCP tools:**
Add methods to `OpenOncoMCPServer` with `@Tool` annotation and `@ToolArg` for parameters. The Quarkus MCP extension automatically registers them.
