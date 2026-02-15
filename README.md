# Eclipse MCP Server

An Eclipse IDE plugin that lets [Claude Code](https://claude.ai/code) control your Eclipse workspace via the [Model Context Protocol](https://modelcontextprotocol.io/) (MCP). Claude Code can list projects, build, launch, read console output, and more — all without leaving the terminal.

## How it works

```
Claude Code → HTTP POST /mcp → Eclipse Plugin (port 5188)
```

The plugin runs an HTTP server inside Eclipse. Claude Code connects directly via HTTP — no bridge, no intermediate process.

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

### 1. Install the plugin

Open the `plugin/` folder as an Eclipse project so Eclipse compiles it, then:

```
cd plugin
build-plugin.bat
```

This creates the plugin JAR and copies it to your Eclipse `dropins/` directory. Restart Eclipse.

### 2. Configure Claude Code

```
claude mcp add eclipse --transport http http://localhost:5188/mcp
```

Or add a `.mcp.json` to your project root (or `~/.claude/mcp.json` for global config):

```json
{
  "mcpServers": {
    "eclipse": {
      "url": "http://localhost:5188/mcp"
    }
  }
}
```

### 3. Verify

```
curl -X POST http://localhost:5188/mcp -H "Content-Type: application/json" -d '{"jsonrpc":"2.0","id":1,"method":"ping"}'
```

You should get back `{"jsonrpc":"2.0","id":1,"result":{}}`.

## Adding custom tools

1. Create a class in `plugin/src/eclipse/mcp/tools/` implementing `IMcpTool`
2. Register it in the `ToolRegistry` constructor

See existing tools for examples.

## License

MIT
