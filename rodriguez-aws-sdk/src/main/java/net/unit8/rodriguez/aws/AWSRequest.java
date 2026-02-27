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

/**
 * Represents a parsed AWS HTTP request, encapsulating the HTTP method, URI, body, and parameters.
 *
 * <p>Instances are created via the {@link #of(HttpExchange, RequestParams)} or
 * {@link #of(HttpExchange)} factory methods.</p>
 */
public class AWSRequest implements Serializable {
    /** The parsed request parameters. */
    private final RequestParams params;
    /** The HTTP method of the request. */
    private final HttpMethod method;
    /** The request URI. */
    private final URI requestURI;
    /** The raw request body input stream. */
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

    /**
     * Returns the HTTP method of this request.
     *
     * @return the HTTP method
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Returns the request body as an input stream.
     *
     * @return the body input stream
     */
    public InputStream getBody() {
        return body;
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
     * Returns the parsed request parameters.
     *
     * @return the request parameters
     */
    public RequestParams getParams() {
        return params;
    }

    /**
     * Creates an {@code AWSRequest} from the given HTTP exchange and pre-parsed parameters.
     *
     * @param exchange the HTTP exchange
     * @param params   the pre-parsed request parameters
     * @return a new {@code AWSRequest} instance
     */
    public static AWSRequest of(HttpExchange exchange, RequestParams params) {
        URI requestURI = exchange.getRequestURI();
        HttpMethod method = HttpMethod.valueOf(exchange.getRequestMethod());
        return new AWSRequest(method, requestURI, exchange.getRequestBody(), params);
    }

    /**
     * Creates an {@code AWSRequest} from the given HTTP exchange, automatically parsing
     * parameters from the query string and form-encoded body.
     *
     * @param exchange the HTTP exchange
     * @return a new {@code AWSRequest} instance
     */
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
