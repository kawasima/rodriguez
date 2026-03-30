package net.unit8.rodriguez.proxy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Serves static files for the proxy dashboard UI from classpath resources.
 *
 * <p>Files are loaded from {@code /static/proxy/} on the classpath.
 */
public class StaticFileHandler implements HttpHandler {
    private static final String RESOURCE_PREFIX = "/static/proxy";

    /** Creates a new static file handler. */
    public StaticFileHandler() {
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String resourcePath = path.replace("/_proxy/ui", RESOURCE_PREFIX);

            if (resourcePath.endsWith("/")) {
                resourcePath += "index.html";
            }

            if (resourcePath.contains("..") || !resourcePath.startsWith(RESOURCE_PREFIX + "/")) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                byte[] content = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", guessContentType(resourcePath));
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(content);
                }
            }
        } finally {
            exchange.close();
        }
    }

    private String guessContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
