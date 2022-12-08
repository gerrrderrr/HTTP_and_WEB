import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {
    private static final String PATH_TO_SETTINGS = "src/main/resources/settings.txt";
    private static final int LIMIT = 4096;
    private static final int THREAD_POOL_SIZE = 64;
    private final Map<String, Map<String, MyHandler>> handlers;
    List<String> allowedMethods;
    private BufferedInputStream in;
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
        in = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        executor.execute(new Thread(() -> {
            try {
                Request request = formRequest(in);
                assert request != null;
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

    private Request formRequest(BufferedInputStream in) throws IOException, URISyntaxException {
        in.mark(LIMIT);
        byte[] buffer = new byte[LIMIT];
        int read = in.read(buffer);

        byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
        int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            return null;
        }

        final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            return null;
        }

        List<String> allowedMethods = handlers.keySet().stream().toList();
        final String method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            return null;
        }

        String requestTarget = requestLine[1];
        if (!requestTarget.startsWith("/")) {
            return null;
        }

        String protocol = requestLine[2];
        if (!protocol.startsWith("HTTP")) {
            return null;
        }

        final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final int headersStart = requestLineEnd + requestLineDelimiter.length;
        final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);

        if (headersEnd == -1) {
            return null;
        }

        in.reset();
        in.skip(headersStart);

        byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
        List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        String body = null;
        if (!method.equals("GET")) {
            in.skip(headersDelimiter.length);
            Optional<String> contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final int length = Integer.parseInt(contentLength.get());
                final byte[] bodyBytes = in.readNBytes(length);
                body = new String(bodyBytes);
            }
        }
        Request request = Request.createRequest(method, requestTarget, body);
        String result = """
                Query params: %s
                Query params named "value": %s
                Query params named: "title": %s
                Body params: %s
                Body params named "login": %s
                Body params named "password": %s
                """.formatted(
                request.getQueryParams()
                , request.getQueryParam("value")
                , request.getQueryParam("title")
                , request.getPostParams()
                , request.getPostParam("login")
                , request.getPostParam("password"));
        System.out.println(result);
        return request;
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
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
