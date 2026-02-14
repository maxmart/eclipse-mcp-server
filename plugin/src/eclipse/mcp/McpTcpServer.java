package eclipse.mcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class McpTcpServer {

    static final int PORT = 5188;
    private ServerSocket serverSocket;
    private volatile boolean running;
    private Thread acceptThread;

    public void start() {
        running = true;
        acceptThread = new Thread(this::acceptLoop, "MCP-TCP-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        try {
            serverSocket = new ServerSocket(PORT, 5, InetAddress.getByName("127.0.0.1"));
            while (running) {
                Socket clientSocket = serverSocket.accept();
                McpClientHandler handler = new McpClientHandler(clientSocket);
                handler.start();
            }
        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }
}
