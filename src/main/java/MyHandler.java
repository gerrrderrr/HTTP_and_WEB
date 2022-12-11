import java.io.BufferedOutputStream;
import java.io.IOException;

@FunctionalInterface
public interface MyHandler {
    void handle(Request request, BufferedOutputStream responseStream) throws IOException;
}