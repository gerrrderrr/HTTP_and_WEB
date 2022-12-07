import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
    private static final String PATH_TO_SETTINGS = "src/main/resources/settings.txt";
    private static final int THREAD_POOL_SIZE = 64;
    private final Map<String, Map<String, MyHandler>> handlers;
    private BufferedReader in;
    private BufferedOutputStream out;

    public Server() {
        handlers = new HashMap<>();
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
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedOutputStream(socket.getOutputStream());
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        executor.execute(new Thread(() -> {
            try {
                final String requestLine = in.readLine();
                final String[] parts = requestLine.split(" ");

                if (parts.length != 3) {
                    Thread.currentThread().interrupt();
                }
                Request request = Request.createRequest(parts[0], parts[1]);
                List<String> listOfSomeParams = request.getQueryParam("value");
                List<String> listOfAllParams = request.getQueryParams();
                System.out.println(listOfAllParams + "\n" + listOfSomeParams);

                if (handlers.containsKey(request.getMethod())) {
                    if (handlers.get(request.getMethod()).containsKey(request.getPath())) {
                        handlers.get(request.getMethod()).get(request.getPath()).handle(request, out);
                    } else {
                        notAvailableHandler("404", "Not Found");
                    }
                } else {
                    notAvailableHandler("404", "Not Found");
                }
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
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

    public void notAvailableHandler(String errorCode, String statusMessage) throws IOException {
        out.write((
                "HTTP/1.1 " + errorCode + " " +
                        statusMessage + "\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }


    public void basicHandler(String path) throws IOException {
        Path filePath = returnPath(path);
        String mimeType = returnMimeType(filePath);
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
    }

    public void addHandler(String method, String path, MyHandler handler) {
        if (!handlers.containsKey(method)) {
            handlers.put(method, new HashMap<>());
            handlers.get(method).put(path, handler);
        } else {
            if (!handlers.get(method).containsKey(path)) {
                handlers.get(method).put(path, handler);
            } else {
                System.out.println("Handler exist");
            }
        }
    }

    private Path returnPath(String path) {
        return Path.of("src/main/resources", path);
    }

    private String returnMimeType(Path filePath) throws IOException {
        return Files.probeContentType(filePath);
    }
}
