package eclipse.mcp.tools;

import com.google.gson.JsonObject;

public interface IMcpTool {

    String getName();

    String getDescription();

    JsonObject getInputSchema();

    JsonObject execute(JsonObject arguments) throws Exception;
}
