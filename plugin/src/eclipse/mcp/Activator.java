package eclipse.mcp;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "eclipse.mcp.server";
    private static Activator instance;
    private McpHttpServer httpServer;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        instance = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        stopServer();
        instance = null;
        super.stop(context);
    }

    public void startServer() {
        if (httpServer == null) {
            httpServer = new McpHttpServer();
            try {
                httpServer.start();
                getLog().info("MCP HTTP server started on port " + McpHttpServer.PORT);
            } catch (Exception e) {
                getLog().error("Failed to start MCP HTTP server", e);
                httpServer = null;
            }
        }
    }

    public void stopServer() {
        if (httpServer != null) {
            httpServer.shutdown();
            httpServer = null;
        }
    }

    public static Activator getInstance() {
        return instance;
    }
}
