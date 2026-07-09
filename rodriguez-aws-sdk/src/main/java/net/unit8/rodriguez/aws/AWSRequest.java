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
    /**
     * Maximum number of bytes read from a request body into memory.
     *
     * <p>Guards the mock against out-of-memory conditions caused by unbounded
     * uploads. Defaults to 64 MB.</p>
     */
    public static final int MAX_BODY_SIZE = 64 * 1024 * 1024;

    /**
     * Reads the given input stream fully into memory, rejecting bodies larger
     * than {@link #MAX_BODY_SIZE}.
     *
     * @param in the input stream to read
     * @return the body bytes
     * @throws IOException            if an I/O error occurs
     * @throws BodyTooLargeException  if the body exceeds {@link #MAX_BODY_SIZE}
     */
    public static byte[] readBoundedBytes(InputStream in) throws IOException {
        byte[] data = in.readNBytes(MAX_BODY_SIZE + 1);
        if (data.length > MAX_BODY_SIZE) {
            throw new BodyTooLargeException(
                    "Request body exceeds the maximum allowed size of " + MAX_BODY_SIZE + " bytes");
        }
        return data;
    }

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
                                return new RequestParams(new String(readBoundedBytes(body), StandardCharsets.UTF_8));
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
