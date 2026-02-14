package eclipse.mcp;

import java.io.*;
import java.net.Socket;

public class McpClientHandler {

    private final Socket socket;
    private final McpProtocolHandler protocol;
    private Thread readerThread;

    public McpClientHandler(Socket socket) {
        this.socket = socket;
        this.protocol = new McpProtocolHandler();
    }

    public void start() {
        readerThread = new Thread(this::readLoop, "MCP-Client-" + socket.getPort());
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String response = protocol.handleMessage(line);
                if (response != null) {
                    writer.write(response);
                    writer.newLine();
                    writer.flush();
                }
            }
        } catch (IOException e) {
            // Client disconnected
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
