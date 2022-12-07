import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class Request {
    private final String method;
    private final String path;

    private List<String> queryList;

    private Request(String requestMethod, String requestPath) {
        this.method = requestMethod;
        this.path = requestPath;
    }

    private Request(String requestMethod, String requestPath, List<String> query) {
        this.method = requestMethod;
        this.path = requestPath;
        this.queryList = query;
    }

    public static Request createRequest(String method, String path) throws URISyntaxException {
        URI uri = new URI(path);
        if (!path.contains("?")) {
            return new Request(method, path);
        } else {
            List<String> query = Arrays.stream(uri.getQuery().split("&")).toList();
            return new Request(method, uri.getPath(), query);
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getQueryParams() {
        if (queryList.isEmpty()) {
            return null;
        } else {
            return queryList;
        }
    }

    public List<String> getQueryParam(String name) {
        if (queryList.isEmpty()) {
            return null;
        } else {
            return queryList.stream().filter(names -> names.contains(name)).toList();
        }
    }
}
