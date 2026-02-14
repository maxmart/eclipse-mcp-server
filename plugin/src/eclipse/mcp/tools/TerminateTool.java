package eclipse.mcp.tools;

import com.google.gson.JsonObject;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;

public class TerminateTool implements IMcpTool {

    @Override
    public String getName() {
        return "terminate";
    }

    @Override
    public String getDescription() {
        return "Terminate running launches. If 'name' is provided, only terminates launches matching that configuration name. Otherwise terminates all.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();

        JsonObject nameProp = new JsonObject();
        nameProp.addProperty("type", "string");
        nameProp.addProperty("description", "Optional: name of the launch configuration to terminate. If omitted, terminates all.");
        props.add("name", nameProp);

        schema.add("properties", props);
        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        String name = arguments.has("name") ? arguments.get("name").getAsString() : null;

        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunch[] launches = manager.getLaunches();
        int terminated = 0;

        for (ILaunch launch : launches) {
            if (!launch.isTerminated()) {
                if (name == null || launch.getLaunchConfiguration().getName().equals(name)) {
                    launch.terminate();
                    terminated++;
                }
            }
        }

        JsonObject result = new JsonObject();
        result.addProperty("terminated", terminated);
        return result;
    }
}
