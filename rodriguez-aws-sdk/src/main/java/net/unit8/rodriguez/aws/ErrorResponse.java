package net.unit8.rodriguez.aws;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Represents an HTTP error response that can be sent back to the client.
 */
public class ErrorResponse {
    private final int statusCode;
    private final String body;
    private ErrorResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    /**
     * Creates a 404 Not Found error response.
     *
     * @return a new {@code ErrorResponse} with status 404
     */
    public static ErrorResponse notFound() {
        return new ErrorResponse(404, "not found");
    }

    /**
     * Writes this error response to the given HTTP exchange.
     *
     * @param exchange the HTTP exchange to write the error response to
     * @throws IOException if an I/O error occurs while sending the response
     */
    public void handle(HttpExchange exchange) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bodyBytes.length);
        exchange.getResponseBody().write(bodyBytes);
    }
}
