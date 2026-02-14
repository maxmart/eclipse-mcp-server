package eclipse.mcp;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "eclipse.mcp.server";
    private static Activator instance;
    private McpTcpServer tcpServer;

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
        if (tcpServer == null) {
            tcpServer = new McpTcpServer();
            tcpServer.start();
            getLog().info("MCP TCP server started on port " + McpTcpServer.PORT);
        }
    }

    public void stopServer() {
        if (tcpServer != null) {
            tcpServer.shutdown();
            tcpServer = null;
        }
    }

    public static Activator getInstance() {
        return instance;
    }
}
