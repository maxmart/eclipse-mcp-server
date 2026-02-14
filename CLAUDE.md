# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Eclipse MCP Server — an Eclipse IDE plugin that exposes Eclipse workspace operations (projects, builds, launches) to Claude Code via the Model Context Protocol (MCP). Communication uses JSON-RPC 2.0 over TCP (port 5188), with a stdio-to-TCP bridge for Claude Code integration.

## Build Commands

**Bridge** (stdio-to-TCP adapter):
```
cd bridge && build-bridge.bat
```
Compiles `StdioBridge.java` and produces `eclipse-mcp-bridge.jar`.

**Plugin** (Eclipse IDE plugin):
```
cd plugin && build-plugin.bat
```
Requires the `plugin/bin/` directory to already exist with compiled classes (open the project in Eclipse first so it compiles via the Eclipse JDT builder). Produces `eclipse.mcp.server_<version>.jar` (version read from `VERSION` file) and copies it to the Eclipse dropins directory. Eclipse must be restarted to load changes.

There are no automated tests, linting, or formatting tools configured.

## Architecture

```
Claude Code (stdio)  →  StdioBridge (bridge/)  →  TCP :5188  →  McpTcpServer (plugin/)
                                                                       ↓
                                                              McpClientHandler (per connection)
                                                                       ↓
                                                              McpProtocolHandler (JSON-RPC dispatch)
                                                                       ↓
                                                              ToolRegistry → IMcpTool implementations
                                                                       ↓
                                                              Eclipse Platform APIs
```

**Two separate Java components:**

- **`bridge/`** — Standalone Java program (no Eclipse dependencies). Reads stdin, forwards to TCP socket at localhost:5188, and pipes responses back to stdout. This is what Claude Code spawns via `.mcp.json`.

- **`plugin/`** — Eclipse OSGi plugin (requires JavaSE-17). Auto-starts via `StartupHook` (registered as `org.eclipse.ui.startup` extension). The `Activator` manages the TCP server lifecycle.

## Key Abstractions

- **`IMcpTool`** (`plugin/src/eclipse/mcp/tools/IMcpTool.java`) — Interface all tools implement: `getName()`, `getDescription()`, `getInputSchema()`, `execute(JsonObject)`.
- **`ToolRegistry`** — Instantiates and indexes all tools by name. Called by `McpProtocolHandler` for `tools/list` and `tools/call`.
- **`McpProtocolHandler`** — Handles JSON-RPC 2.0 methods: `initialize`, `tools/list`, `tools/call`, `ping`. Notifications (no `id`) are silently ignored.

## Adding a New Tool

1. Create a class in `plugin/src/eclipse/mcp/tools/` implementing `IMcpTool`.
2. Register it in `ToolRegistry` constructor with `register(new YourTool())`.

The tool's `getInputSchema()` must return a valid JSON Schema object. The `execute()` method receives the parsed arguments and returns a `JsonObject` result.

## Important Details

- TCP server binds to `127.0.0.1:5188` (localhost only) — hardcoded in `McpTcpServer.PORT`.
- Bridge retries connection to the TCP server for up to 30 seconds on startup.
- Thread-per-client model: each TCP connection gets a daemon thread in `McpClientHandler`.
- Many tools run Eclipse operations on the UI thread via `Display.getDefault().syncExec()` or use the Eclipse Jobs API for async work (builds, refreshes).
- The plugin depends on `com.google.gson` for all JSON handling.
- MCP protocol version: `2024-11-05`.
