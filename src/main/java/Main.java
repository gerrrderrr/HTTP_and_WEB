import java.util.List;

public class Main {
    final static List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    public static void main(String[] args) {
        Server server = new Server();
        for (String path : validPaths) {
            server.addHandler("GET", path, (request, responseStream) -> server.basicHandler(path));
        }
        server.serverStart();
    }
}


