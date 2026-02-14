package eclipse.mcp.bridge;

import java.io.*;
import java.net.Socket;

/**
 * Stdio-to-TCP bridge for Eclipse MCP Server.
 * Claude Code launches this as a subprocess (stdio), and it forwards
 * JSON-RPC messages to/from the Eclipse plugin's TCP server on localhost:5188.
 */
public class StdioBridge {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 5188;
    private static final int RETRY_INTERVAL_MS = 1000;
    private static final int MAX_RETRY_MS = 30000;

    public static void main(String[] args) throws Exception {
        Socket socket = connectWithRetry();

        InputStream socketIn = socket.getInputStream();
        OutputStream socketOut = socket.getOutputStream();
        InputStream stdIn = System.in;
        OutputStream stdOut = System.out;

        // stdin -> socket (in a background thread)
        Thread stdinToSocket = new Thread(() -> {
            try {
                pipe(stdIn, socketOut);
            } catch (IOException e) {
                // Connection closed
            } finally {
                System.exit(0);
            }
        }, "stdin-to-socket");
        stdinToSocket.setDaemon(true);
        stdinToSocket.start();

        // socket -> stdout (in the main thread)
        try {
            pipe(socketIn, stdOut);
        } catch (IOException e) {
            // Connection closed
        } finally {
            System.exit(0);
        }
    }

    private static Socket connectWithRetry() {
        long deadline = System.currentTimeMillis() + MAX_RETRY_MS;
        while (true) {
            try {
                return new Socket(HOST, PORT);
            } catch (IOException e) {
                if (System.currentTimeMillis() >= deadline) {
                    System.err.println("Failed to connect to Eclipse MCP server at " + HOST + ":" + PORT);
                    System.exit(1);
                }
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.exit(1);
                }
            }
        }
    }

    private static void pipe(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            out.flush();
        }
    }
}
