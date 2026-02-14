package eclipse.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;

public class ListLaunchesTool implements IMcpTool {

    @Override
    public String getName() {
        return "list_launches";
    }

    @Override
    public String getDescription() {
        return "List all active and recent launches with their status (running or terminated).";
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
        ILaunch[] launches = manager.getLaunches();
        JsonArray list = new JsonArray();
        for (ILaunch launch : launches) {
            JsonObject l = new JsonObject();
            l.addProperty("name", launch.getLaunchConfiguration().getName());
            l.addProperty("mode", launch.getLaunchMode());
            l.addProperty("terminated", launch.isTerminated());
            list.add(l);
        }
        JsonObject result = new JsonObject();
        result.add("launches", list);
        return result;
    }
}
