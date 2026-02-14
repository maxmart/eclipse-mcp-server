package eclipse.mcp.tools;

import com.google.gson.JsonObject;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.TextConsole;

public class GetConsoleOutputTool implements IMcpTool {

    @Override
    public String getName() {
        return "get_console_output";
    }

    @Override
    public String getDescription() {
        return "Get the console output (stdout and stderr) of the most recent launch, or a specific launch by name.";
    }

    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        JsonObject props = new JsonObject();

        JsonObject nameProp = new JsonObject();
        nameProp.addProperty("type", "string");
        nameProp.addProperty("description", "Optional: name of the launch configuration. If omitted, uses the most recent launch.");
        props.add("name", nameProp);

        schema.add("properties", props);
        return schema;
    }

    @Override
    public JsonObject execute(JsonObject arguments) throws Exception {
        String name = arguments.has("name") ? arguments.get("name").getAsString() : null;

        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
        ILaunch[] launches = manager.getLaunches();

        // Find the target launch (most recent matching, or most recent overall)
        ILaunch target = null;
        for (int i = launches.length - 1; i >= 0; i--) {
            ILaunch launch = launches[i];
            if (name == null || launch.getLaunchConfiguration().getName().equals(name)) {
                target = launch;
                break;
            }
        }

        if (target == null) {
            throw new IllegalArgumentException(name != null
                    ? "No launch found for: " + name
                    : "No launches found");
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        for (IProcess process : target.getProcesses()) {
            IStreamsProxy proxy = process.getStreamsProxy();
            if (proxy != null) {
                IStreamMonitor outMonitor = proxy.getOutputStreamMonitor();
                if (outMonitor != null) {
                    stdout.append(outMonitor.getContents());
                }
                IStreamMonitor errMonitor = proxy.getErrorStreamMonitor();
                if (errMonitor != null) {
                    stderr.append(errMonitor.getContents());
                }
            }
        }

        // Fallback: Eclipse's Console view calls setBuffered(false) on stream
        // monitors after attaching its own listener, which clears the buffer.
        // When that happens, getContents() returns empty. Read from the Console
        // document instead (the same text the user sees in the Console view).
        if (stdout.length() == 0 && stderr.length() == 0) {
            IConsoleManager consoleMgr = ConsolePlugin.getDefault().getConsoleManager();
            for (org.eclipse.ui.console.IConsole console : consoleMgr.getConsoles()) {
                if (console instanceof org.eclipse.debug.ui.console.IConsole
                        && console instanceof TextConsole) {
                    IProcess consoleProcess =
                            ((org.eclipse.debug.ui.console.IConsole) console).getProcess();
                    for (IProcess targetProcess : target.getProcesses()) {
                        if (consoleProcess.equals(targetProcess)) {
                            String text = ((TextConsole) console).getDocument().get();
                            if (text != null && !text.isEmpty()) {
                                stdout.append(text);
                            }
                        }
                    }
                }
            }
        }

        JsonObject result = new JsonObject();
        result.addProperty("configName", target.getLaunchConfiguration().getName());
        result.addProperty("terminated", target.isTerminated());
        result.addProperty("stdout", stdout.toString());
        result.addProperty("stderr", stderr.toString());
        return result;
    }
}
