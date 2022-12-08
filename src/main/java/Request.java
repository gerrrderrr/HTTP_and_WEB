import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Request {
    private final String method;
    private final String path;
    private final List<String> bodyList;

    private final List<String> queryList;

    private Request(String requestMethod, String requestPath, List<String> query, List<String> body) {
        this.method = requestMethod;
        this.path = requestPath;
        this.queryList = query;
        this.bodyList = body;
    }

    public static Request createRequest(String method, String path, String body) throws URISyntaxException {
        URI uri = new URI(path);
        List<String> bodyDecodedList = null;
        List<String> query = null;
        if (body != null) {
            String decodedBody = URLDecoder.decode(body, UTF_8);
            bodyDecodedList = Arrays.stream(decodedBody.split("&")).toList();   
        }
        if (path.contains("?")) {
            query = Arrays.stream(uri.getQuery().split("&")).toList();
        }
        
        return new Request(method, uri.getPath(), query, bodyDecodedList);
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getQueryParams() {
        return queryList;
    }

    public List<String> getQueryParam(String name) {
        if (queryList == null) {
            return null;
        } else {
            return queryList.stream()
                    .filter(names -> names.contains(name))
                    .flatMap(o -> o.substring(o.indexOf("=") + 1).trim().lines()).toList();
        }
    }

    public List<String> getPostParams() {
        return bodyList;
    }
    public List<String> getPostParam(String name) {
        if (bodyList == null) {
            return null;
        } else {
            return bodyList.stream()
                    .filter(names -> names.contains(name))
                    .flatMap(o -> o.substring(o.indexOf("=") + 1).trim().lines()).toList();
        }
    }
}
