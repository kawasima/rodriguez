package net.unit8.rodriguez.aws;

import com.amazonaws.HttpMethod;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class AWSRequest implements Serializable {
    private final RequestParams params;
    private final HttpMethod method;
    private final URI requestURI;
    private final InputStream body;

    private AWSRequest(HttpMethod method,
                       URI requestURI,
                       InputStream body,
                       RequestParams params) {
        this.params = params;
        this.method = method;
        this.requestURI = requestURI;
        this.body = body;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public InputStream getBody() {
        return body;
    }

    public URI getRequestURI() {
        return requestURI;
    }

    public RequestParams getParams() {
        return params;
    }

    public static AWSRequest of(HttpExchange exchange) {
        URI requestURI = exchange.getRequestURI();
        HttpMethod method = HttpMethod.valueOf(exchange.getRequestMethod());
        InputStream body = exchange.getRequestBody();

        Headers headers = exchange.getRequestHeaders();
        RequestParams params = Stream.of(Optional.ofNullable(requestURI.getQuery())
                        .map(RequestParams::new)
                        .orElse(null),
                Optional.ofNullable(headers.getFirst("content-type"))
                        .filter(contentType -> contentType.startsWith("application/x-www-form-urlencoded"))
                        .map(type -> {
                            try {
                                return new RequestParams(new String(body.readAllBytes(), StandardCharsets.UTF_8));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        })
                        .orElse(null)
        )
                .filter(Objects::nonNull)
                .reduce(RequestParams::merge)
                .orElse(new RequestParams());
        return new AWSRequest(method, requestURI, body, params);
    }
}
