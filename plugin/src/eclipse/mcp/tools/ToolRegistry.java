package eclipse.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class ToolRegistry {

    private final Map<String, IMcpTool> tools = new LinkedHashMap<>();

    public ToolRegistry() {
        register(new ListProjectsTool());
        register(new ListLaunchConfigsTool());
        register(new ListLaunchesTool());
        register(new LaunchTool());
        register(new TerminateTool());
        register(new RefreshProjectTool());
        register(new BuildProjectTool());
        register(new GetConsoleOutputTool());
        register(new RunTool());
    }

    private void register(IMcpTool tool) {
        tools.put(tool.getName(), tool);
    }

    public JsonArray listToolSchemas() {
        JsonArray array = new JsonArray();
        for (IMcpTool tool : tools.values()) {
            JsonObject schema = new JsonObject();
            schema.addProperty("name", tool.getName());
            schema.addProperty("description", tool.getDescription());
            schema.add("inputSchema", tool.getInputSchema());
            array.add(schema);
        }
        return array;
    }

    public JsonObject callTool(String name, JsonObject arguments) throws Exception {
        IMcpTool tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        return tool.execute(arguments);
    }
}
