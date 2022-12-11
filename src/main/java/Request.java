import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ParameterParser;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

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
    private final List<FileItem> items;

    private Request(String requestMethod, String requestPath, List<String> query, List<String> body, List<FileItem> items) {
        this.method = requestMethod;
        this.path = requestPath;
        this.queryList = query;
        this.bodyList = body;
        this.items = items;
    }

    public static Request createRequest(String method, String path, String body, String contentType) throws URISyntaxException, FileUploadException {
        boolean isMultipart = contentType.contains("multipart/form-data");
        URI uri = new URI(path);
        List<String> query = null;
        if (path.contains("?")) {
            query = Arrays.stream(uri.getQuery().split("&")).toList();
        }
        if (!isMultipart && body != null) {
            String decodedBody = URLDecoder.decode(body, UTF_8);
            List<String> bodyDecodedList = Arrays.stream(decodedBody.split("&")).toList();
            return new Request(method, uri.getPath(), query, bodyDecodedList, null);
        } else if (isMultipart) {
            ParameterParser parameterParser = new ParameterParser();
            parameterParser.setLowerCaseNames(true);
            String charset = parameterParser.parse(contentType, ';').get("charset");
            RequestContextImpl requestContext = new RequestContextImpl(charset, contentType, body.getBytes());
            FileUpload upload = new FileUpload();
            DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
            upload.setFileItemFactory(diskFileItemFactory);
            List<FileItem> files = upload.parseRequest(requestContext);
            return new Request(method, uri.getPath(), query, null, files);
        }
        return new Request(method, uri.getPath(), query, null, null);
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

    public List<FileItem> getParts() {
        return items;
    }

    public List<FileItem> getPart(String name) {
        if (items == null) {
            return null;
        } else {
            return items.stream().filter(names -> names.getFieldName().contains(name)).toList();
        }
    }
}

