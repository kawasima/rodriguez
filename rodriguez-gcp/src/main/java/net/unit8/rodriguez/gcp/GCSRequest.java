package net.unit8.rodriguez.gcp;

import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A lightweight HTTP request wrapper for the GCS mock server.
 *
 * <p>Wraps {@link HttpExchange} and extracts the HTTP method, URI, query parameters,
 * headers, and request body for use by GCS action handlers.
 */
public class GCSRequest {
    private final String method;
    private final URI requestURI;
    private InputStream body;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;

    private GCSRequest(String method, URI requestURI, InputStream body,
                       Map<String, String> queryParams, Map<String, String> headers) {
        this.method = method;
        this.requestURI = requestURI;
        this.body = body;
        this.queryParams = queryParams;
        this.headers = headers;
    }

    /**
     * Returns the HTTP method (GET, POST, PUT, DELETE, etc.).
     *
     * @return the HTTP method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the request URI.
     *
     * @return the request URI
     */
    public URI getRequestURI() {
        return requestURI;
    }

    /**
     * Returns the request body as an input stream.
     *
     * @return the request body
     */
    public InputStream getBody() {
        return body;
    }

    /**
     * Returns a single query parameter value.
     *
     * @param key the parameter name
     * @return the parameter value, or {@code null} if not present
     */
    public String getQueryParam(String key) {
        return queryParams.get(key);
    }

    /**
     * Returns the mutable map of all query parameters.
     *
     * @return the query parameters map
     */
    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    /**
     * Returns a request header value by name (case-insensitive).
     *
     * @param key the header name
     * @return the header value, or {@code null} if not present
     */
    public String getHeader(String key) {
        return headers.get(key);
    }

    /**
     * Replaces the request body stream.
     *
     * @param body the new body input stream
     */
    public void setBody(InputStream body) {
        this.body = body;
    }

    /**
     * Creates a {@code GCSRequest} from an {@link HttpExchange}.
     *
     * @param exchange the HTTP exchange to wrap
     * @return a new GCSRequest instance
     */
    public static GCSRequest of(HttpExchange exchange) {
        URI requestURI = exchange.getRequestURI();
        String method = exchange.getRequestMethod();
        InputStream body = exchange.getRequestBody();
        Map<String, String> queryParams = parseQuery(requestURI.getQuery());
        Map<String, String> headers = new LinkedHashMap<>();
        exchange.getRequestHeaders().forEach((key, values) -> {
            if (!values.isEmpty()) {
                headers.put(key.toLowerCase(), values.getFirst());
            }
        });
        return new GCSRequest(method, requestURI, body, queryParams, headers);
    }

    private static Map<String, String> parseQuery(String query) {
        if (query == null || query.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, String> params = new LinkedHashMap<>();
        for (String param : query.split("&")) {
            int idx = param.indexOf('=');
            String key = idx > 0 ? param.substring(0, idx) : param;
            String value = idx > 0 && param.length() > idx + 1 ? param.substring(idx + 1) : "";
            params.put(
                    URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8)
            );
        }
        return params;
    }
}
