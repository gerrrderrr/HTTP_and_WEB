import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
    private static final String PATH_TO_SETTINGS = "src/main/resources/settings.txt";
    private static final int THREAD_POOL_SIZE = 64;

    public Server() {
    }

    public void serverStart() {
        final int port = readPort();
        if (port != 0) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    taskStart(socket);
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void taskStart(Socket socket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        executor.execute(new Thread(() -> {
            try {
                final String requestLine = in.readLine();
                final String[] parts = requestLine.split(" ");

                if (parts.length != 3) {
                    Thread.currentThread().interrupt();
                }

                final String path = parts[1];
                if (!Main.validPaths.contains(path)) {
                    out.write((
                            """
                                    HTTP/1.1 404 Not Found\r
                                    Content-Length: 0\r
                                    Connection: close\r
                                    \r
                                    """
                    ).getBytes());
                    out.flush();
                }

                final Path filePath = Path.of("src/main/resources", path);
                final String mimeType = Files.probeContentType(filePath);

                if (path.equals("/classic.html")) {
                    final String template = Files.readString(filePath);
                    final byte[] content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.write(content);
                    out.flush();
                }

                final long length = Files.size(filePath);
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, out);
                out.flush();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }));
    }

    private int readPort() {
        int port = 0;
        try (FileInputStream portFromFile = new FileInputStream(PATH_TO_SETTINGS)) {
            byte[] portInBytes = portFromFile.readAllBytes();
            String portRead = new String(portInBytes);
            port = Integer.parseInt(portRead);
        } catch (IOException ignored) {
        }
        if (port == 0) {
            System.out.println("Failed to read the port");
            return 0;
        } else {
            return port;
        }
    }
}
