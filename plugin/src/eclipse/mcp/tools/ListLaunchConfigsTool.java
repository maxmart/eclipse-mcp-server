package eclipse.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;

public class ListLaunchConfigsTool implements IMcpTool {

    @Override
    public String getName() {
        return "list_launch_configs";
    }

    @Override
    public String getDescription() {
        return "List all launch configurations in Eclipse";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfiguration[] configs = manager.getLaunchConfigurations();
        JsonArray list = new JsonArray();
        for (ILaunchConfiguration config : configs) {
            JsonObject c = new JsonObject();
            c.addProperty("name", config.getName());
            c.addProperty("type", config.getType().getName());
            list.add(c);
        }
        JsonObject result = new JsonObject();
        result.add("launchConfigurations", list);
        return result;
    }
}
