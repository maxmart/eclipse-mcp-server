# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Eclipse MCP Server — an Eclipse IDE plugin that exposes Eclipse workspace operations (projects, builds, launches) to Claude Code via the Model Context Protocol (MCP). Communication uses JSON-RPC 2.0 over HTTP (port 5188).

## Build Commands

**Plugin** (Eclipse IDE plugin):
```
cd plugin && build-plugin.bat
```
Requires the `plugin/bin/` directory to already exist with compiled classes (open the project in Eclipse first so it compiles via the Eclipse JDT builder). Produces `eclipse.mcp.server_<version>.jar` (version read from `VERSION` file) and copies it to the Eclipse dropins directory. Eclipse must be restarted to load changes.

There are no automated tests, linting, or formatting tools configured.

## Architecture

```
Claude Code → HTTP POST /mcp → McpHttpServer (plugin/, port 5188)
                                       ↓
                              McpProtocolHandler (JSON-RPC dispatch)
                                       ↓
                              ToolRegistry → IMcpTool implementations
                                       ↓
                              Eclipse Platform APIs
```

**Single component:** `plugin/` — Eclipse OSGi plugin (requires JavaSE-17). Auto-starts via `StartupHook` (registered as `org.eclipse.ui.startup` extension). The `Activator` manages the HTTP server lifecycle.

`McpHttpServer` uses `com.sun.net.httpserver.HttpServer` (built into JDK 17+) with a daemon thread pool. A single shared `McpProtocolHandler` instance handles all requests (it's thread-safe).

## Key Abstractions

- **`IMcpTool`** (`plugin/src/eclipse/mcp/tools/IMcpTool.java`) — Interface all tools implement: `getName()`, `getDescription()`, `getInputSchema()`, `execute(JsonObject)`.
- **`ToolRegistry`** — Instantiates and indexes all tools by name. Called by `McpProtocolHandler` for `tools/list` and `tools/call`.
- **`McpProtocolHandler`** — Handles JSON-RPC 2.0 methods: `initialize`, `tools/list`, `tools/call`, `ping`. Notifications (no `id`) are silently ignored.

## Adding a New Tool

1. Create a class in `plugin/src/eclipse/mcp/tools/` implementing `IMcpTool`.
2. Register it in `ToolRegistry` constructor with `register(new YourTool())`.

The tool's `getInputSchema()` must return a valid JSON Schema object. The `execute()` method receives the parsed arguments and returns a `JsonObject` result.

## Versioning

- The version is stored in the `VERSION` file at the project root.
- **Always bump the version when making changes** — update `VERSION`, `META-INF/MANIFEST.MF` (`Bundle-Version`), and the JAR filename in `build-plugin.bat`.
- Use semantic versioning: patch for bug fixes, minor for new features/tools, major for breaking changes.

## Important Details

- HTTP server binds to `0.0.0.0:5188` — hardcoded in `McpHttpServer.PORT`.
- Daemon thread pool via `Executors.newCachedThreadPool` — threads are created on demand, reused, and cleaned up after idle.
- Many tools run Eclipse operations on the UI thread via `Display.getDefault().syncExec()` or use the Eclipse Jobs API for async work (builds, refreshes).
- The plugin depends on `com.google.gson` for all JSON handling.
- MCP protocol version: `2024-11-05`.
