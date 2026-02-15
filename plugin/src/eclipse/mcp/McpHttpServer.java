package eclipse.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class McpHttpServer {

    static final int PORT = 5188;
    private HttpServer server;
    private final McpProtocolHandler protocol = new McpProtocolHandler();

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "MCP-HTTP-Worker");
            t.setDaemon(true);
            return t;
        }));
        server.createContext("/mcp", this::handleMcp);
        server.start();
    }

    private void handleMcp(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        byte[] requestBytes;
        try (InputStream is = exchange.getRequestBody()) {
            requestBytes = is.readAllBytes();
        }
        String json = new String(requestBytes, StandardCharsets.UTF_8);

        String response = protocol.handleMessage(json);

        if (response == null) {
            // Notification — no response body
            exchange.sendResponseHeaders(202, -1);
        } else {
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        exchange.close();
    }

    public void shutdown() {
        if (server != null) {
            server.stop(0);
        }
    }
}
