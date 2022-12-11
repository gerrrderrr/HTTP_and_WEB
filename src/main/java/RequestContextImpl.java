import org.apache.commons.fileupload.RequestContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class RequestContextImpl implements RequestContext {
    private final String charset;
    private final String contentType;
    private final byte[] content;

    public RequestContextImpl(String charset, String contentType, byte[] content) {
        this.charset = charset;
        this.contentType = contentType;
        this.content = content;
    }

    @Override
    public String getCharacterEncoding() {
        return this.charset;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public int getContentLength() {
        return this.content.length;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.content);
    }
}
