package net.unit8.rodriguez.aws;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ErrorResponse {
    private final int statusCode;
    private final String body;
    private ErrorResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public static ErrorResponse notFound() {
        return new ErrorResponse(404, "not found");
    }

    public void handle(HttpExchange exchange) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bodyBytes.length);
        exchange.getResponseBody().write(bodyBytes);
    }
}
