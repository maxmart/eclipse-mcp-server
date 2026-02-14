# Eclipse MCP Server

An Eclipse IDE plugin that lets [Claude Code](https://claude.ai/code) control your Eclipse workspace via the [Model Context Protocol](https://modelcontextprotocol.io/) (MCP). Claude Code can list projects, build, launch, read console output, and more — all without leaving the terminal.

## How it works

```
Claude Code (stdio) → StdioBridge (bridge/) → TCP :5188 → Eclipse Plugin (plugin/)
```

Two Java components:

- **`bridge/`** — Lightweight stdin/stdout-to-TCP pipe. This is what Claude Code spawns. No Eclipse dependencies.
- **`plugin/`** — Eclipse OSGi plugin that accepts TCP connections and dispatches tool calls to Eclipse Platform APIs.

## Available tools

| Tool | Description |
|------|-------------|
| `list_projects` | List all projects in the workspace |
| `list_launch_configs` | List all run/debug configurations |
| `list_launches` | List active and recent launches |
| `launch` | Launch a run configuration (run or debug mode) |
| `terminate` | Terminate running launches |
| `refresh_project` | Refresh project(s) from filesystem (supports targeted file refresh) |
| `build_project` | Build project(s) |
| `get_console_output` | Read stdout/stderr from a launch |
| `run` | All-in-one: terminate + refresh + build + launch |

## Setup

### Prerequisites

- Eclipse IDE (2023-06 or later)
- Java 17+

### 1. Build the bridge

```
cd bridge
build-bridge.bat
```

### 2. Install the plugin

Open the `plugin/` folder as an Eclipse project so Eclipse compiles it, then:

```
cd plugin
build-plugin.bat
```

This creates the plugin JAR and copies it to your Eclipse `dropins/` directory. Restart Eclipse.

### 3. Configure Claude Code

Add a `.mcp.json` to your project root (or `~/.claude/mcp.json` for global config):

```json
{
  "mcpServers": {
    "eclipse": {
      "command": "java",
      "args": ["-cp", "/path/to/eclipse-mcp-server/bridge/out", "eclipse.mcp.bridge.StdioBridge"]
    }
  }
}
```

Replace `/path/to/eclipse-mcp-server` with the actual path on your machine.

## Adding custom tools

1. Create a class in `plugin/src/eclipse/mcp/tools/` implementing `IMcpTool`
2. Register it in the `ToolRegistry` constructor

See existing tools for examples.

## License

MIT
