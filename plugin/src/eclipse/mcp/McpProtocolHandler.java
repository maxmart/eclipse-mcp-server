package eclipse.mcp;

import com.google.gson.*;
import eclipse.mcp.tools.ToolRegistry;

public class McpProtocolHandler {

    private static final String PROTOCOL_VERSION = "2024-11-05";
    private static final Gson GSON = new GsonBuilder().create();
    private final ToolRegistry toolRegistry;

    public McpProtocolHandler() {
        this.toolRegistry = new ToolRegistry();
    }

    public String handleMessage(String json) {
        try {
            JsonObject request = JsonParser.parseString(json).getAsJsonObject();
            String method = request.has("method") ? request.get("method").getAsString() : null;

            // Notifications have no id and expect no response
            if (!request.has("id")) {
                return null;
            }

            JsonElement id = request.get("id");
            JsonObject params = request.has("params") ? request.getAsJsonObject("params") : new JsonObject();

            switch (method) {
                case "initialize":
                    return respondResult(id, buildInitializeResult());
                case "tools/list":
                    return respondResult(id, buildToolsList());
                case "tools/call":
                    return respondResult(id, handleToolCall(params));
                case "ping":
                    return respondResult(id, new JsonObject());
                default:
                    return respondError(id, -32601, "Method not found: " + method);
            }
        } catch (Exception e) {
            try {
                JsonObject request = JsonParser.parseString(json).getAsJsonObject();
                if (request.has("id")) {
                    return respondError(request.get("id"), -32603, e.getMessage());
                }
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    private JsonObject buildInitializeResult() {
        JsonObject result = new JsonObject();
        result.addProperty("protocolVersion", PROTOCOL_VERSION);

        JsonObject capabilities = new JsonObject();
        capabilities.add("tools", new JsonObject());
        result.add("capabilities", capabilities);

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("name", "eclipse-mcp-server");
        serverInfo.addProperty("version", "0.3.0");
        result.add("serverInfo", serverInfo);

        return result;
    }

    private JsonObject buildToolsList() {
        JsonObject result = new JsonObject();
        result.add("tools", toolRegistry.listToolSchemas());
        return result;
    }

    private JsonObject handleToolCall(JsonObject params) {
        String toolName = params.get("name").getAsString();
        JsonObject arguments = params.has("arguments") ? params.getAsJsonObject("arguments") : new JsonObject();

        try {
            JsonObject toolResult = toolRegistry.callTool(toolName, arguments);
            JsonObject result = new JsonObject();
            JsonArray content = new JsonArray();
            JsonObject textContent = new JsonObject();
            textContent.addProperty("type", "text");
            textContent.addProperty("text", GSON.toJson(toolResult));
            content.add(textContent);
            result.add("content", content);
            return result;
        } catch (Exception e) {
            JsonObject result = new JsonObject();
            JsonArray content = new JsonArray();
            JsonObject textContent = new JsonObject();
            textContent.addProperty("type", "text");
            textContent.addProperty("text", "Error: " + e.getMessage());
            content.add(textContent);
            result.add("content", content);
            result.addProperty("isError", true);
            return result;
        }
    }

    private String respondResult(JsonElement id, JsonObject result) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        response.add("result", result);
        return GSON.toJson(response);
    }

    private String respondError(JsonElement id, int code, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", id);
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);
        response.add("error", error);
        return GSON.toJson(response);
    }
}
