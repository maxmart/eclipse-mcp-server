# Eclipse MCP Server

An Eclipse IDE plugin that lets [Claude Code](https://claude.ai/code) control your Eclipse workspace — build, launch, read console output, and more.

## Quick Start

### 1. Download and install

Download `eclipse.mcp.server_0.3.0.jar` from the [latest release](https://github.com/maxmart/eclipse-mcp-server/releases/latest) and copy it into your Eclipse `dropins/` folder:

```
<eclipse-install-dir>/eclipse/dropins/
```

Restart Eclipse (use `-clean` on first install).

### 2. Connect Claude Code

```
claude mcp add eclipse --transport http http://localhost:5188/mcp -s user
```

That's it. Claude Code can now control your Eclipse workspace.

## Available Tools

| Tool | Description |
|------|-------------|
| `list_projects` | List all projects in the workspace |
| `list_launch_configs` | List all run/debug configurations |
| `list_launches` | List active and recent launches |
| `launch` | Launch a run configuration (run or debug mode) |
| `terminate` | Terminate running launches |
| `refresh_project` | Refresh project(s) from filesystem |
| `build_project` | Build project(s) |
| `get_console_output` | Read stdout/stderr from a launch |
| `run` | All-in-one: terminate + refresh + build + launch |

## Building from Source

Requires Java 17+ and Eclipse IDE (2023-06 or later).

```
cd plugin
build-plugin.bat
```

This compiles, builds the JAR, and copies it to your Eclipse `dropins/` directory.

## License

MIT
