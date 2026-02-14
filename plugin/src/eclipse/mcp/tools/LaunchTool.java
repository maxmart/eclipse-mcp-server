package eclipse.mcp.tools;

import com.google.gson.JsonObject;
import org.eclipse.debug.core.*;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class LaunchTool implements IMcpTool {

    @Override
    public String getName() {
        return "launch";
    }

    @Override
    public String getDescription() {
        return "Launch a run configuration by name. Mode can be 'run' or 'debug'.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();

        JsonObject nameProp = new JsonObject();
        nameProp.addProperty("type", "string");
        nameProp.addProperty("description", "Name of the launch configuration");
        props.add("name", nameProp);

        JsonObject modeProp = new JsonObject();
        modeProp.addProperty("type", "string");
        modeProp.addProperty("description", "Launch mode: 'run' or 'debug' (default: 'run')");
        props.add("mode", modeProp);

        schema.add("properties", props);

        com.google.gson.JsonArray required = new com.google.gson.JsonArray();
        required.add("name");
        schema.add("required", required);

        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        String name = arguments.get("name").getAsString();
        String mode = arguments.has("mode") ? arguments.get("mode").getAsString() : ILaunchManager.RUN_MODE;

        ILaunchConfiguration config = findConfig(name);
        if (config == null) {
            throw new IllegalArgumentException("Launch configuration not found: " + name);
        }

        final ILaunchConfiguration cfg = config;
        final String launchMode = mode;
        final ILaunch[] result = new ILaunch[1];
        final Exception[] error = new Exception[1];

        Display display = PlatformUI.getWorkbench().getDisplay();
        display.syncExec(() -> {
            try {
                result[0] = cfg.launch(launchMode, null);
            } catch (Exception e) {
                error[0] = e;
            }
        });

        if (error[0] != null) {
            throw error[0];
        }

        JsonObject out = new JsonObject();
        out.addProperty("launched", name);
        out.addProperty("mode", launchMode);
        return out;
    }

    static ILaunchConfiguration findConfig(String name) throws Exception {
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        for (ILaunchConfiguration config : manager.getLaunchConfigurations()) {
            if (config.getName().equals(name)) {
                return config;
            }
        }
        return null;
    }
}
