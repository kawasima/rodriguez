package net.unit8.rodriguez.proxy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.unit8.rodriguez.proxy.ProxyConfig;
import net.unit8.rodriguez.proxy.model.FaultRule;
import net.unit8.rodriguez.proxy.store.FaultRuleStore;
import net.unit8.rodriguez.proxy.store.ObservedPathStore;
import net.unit8.rodriguez.util.BodyTooLargeException;
import net.unit8.rodriguez.util.BoundedBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reverse proxy handler that forwards requests to upstream or fault ports.
 *
 * <p>When a fault rule matches the request path, the request is forwarded to the
 * corresponding Rodriguez fault port instead of the real upstream service.
 * Paths that return a successful upstream response (HTTP 200-399) are recorded
 * in {@link ObservedPathStore} for display in the Dashboard.
 */
public class ProxyHandler implements HttpHandler {
    private static final Logger LOG = Logger.getLogger(ProxyHandler.class.getName());
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade", "host", "content-length"
    );

    private final HttpClient httpClient;
    private final FaultRuleStore store;
    private final ProxyConfig config;
    private final ObservedPathStore observedPathStore;

    /**
     * Creates a new proxy handler.
     *
     * @param httpClient        HTTP client for forwarding requests
     * @param store             fault rule store
     * @param config            proxy configuration
     * @param observedPathStore store for recording observed paths
     */
    public ProxyHandler(HttpClient httpClient, FaultRuleStore store, ProxyConfig config,
                        ObservedPathStore observedPathStore) {
        this.httpClient = httpClient;
        this.store = store;
        this.config = config;
        this.observedPathStore = observedPathStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Reject internal paths that should be handled by dedicated handlers
        if (path.startsWith("/_proxy/")) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        Optional<FaultRule> matchedRule = store.findAndConsume(path);
        String targetBase;
        if (matchedRule.isPresent()) {
            FaultRule rule = matchedRule.get();
            targetBase = "http://localhost:" + rule.getFaultPort();
            LOG.info("Fault injected: " + method + " " + path + " -> "
                    + rule.getFaultType() + " (port " + rule.getFaultPort() + ")");
        } else {
            targetBase = config.getUpstream();
        }

        // Tracks whether we have already begun the response, so the error handlers below
        // don't attempt to send a 502 status after headers/body have started streaming.
        boolean responseStarted = false;
        try {
            String query = exchange.getRequestURI().getRawQuery();
            String targetUri = targetBase + path + (query != null ? "?" + query : "");

            // Fast-path rejection when Content-Length advertises an oversized body.
            String contentLengthHeader = exchange.getRequestHeaders().getFirst("Content-Length");
            if (contentLengthHeader != null) {
                try {
                    if (Long.parseLong(contentLengthHeader) > config.getMaxRequestBodyBytes()) {
                        exchange.sendResponseHeaders(413, -1);
                        exchange.close();
                        return;
                    }
                } catch (NumberFormatException ignore) {
                    // Malformed header; the bounded read below still enforces the limit.
                }
            }

            // Enforce the limit while reading regardless of Content-Length, so a chunked
            // request with no Content-Length cannot buffer an unbounded body (OOM).
            byte[] requestBody;
            try {
                requestBody = BoundedBody.read(
                        exchange.getRequestBody(), config.getMaxRequestBodyBytes());
            } catch (BodyTooLargeException e) {
                exchange.sendResponseHeaders(413, -1);
                exchange.close();
                return;
            }

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .timeout(Duration.ofMillis(config.getRequestTimeoutMs()));

            exchange.getRequestHeaders().forEach((name, values) -> {
                if (!isHopByHop(name)) {
                    values.forEach(v -> reqBuilder.header(name, v));
                }
            });

            HttpRequest.BodyPublisher bodyPublisher = requestBody.length > 0
                    ? HttpRequest.BodyPublishers.ofByteArray(requestBody)
                    : HttpRequest.BodyPublishers.noBody();
            reqBuilder.method(method, bodyPublisher);

            // Stream the response instead of buffering it fully, so pointing a rule at the
            // OversizedResponse fault port (or any large-body upstream) cannot OOM the proxy.
            HttpResponse<InputStream> upstreamResponse = httpClient.send(
                    reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofInputStream());

            int statusCode = upstreamResponse.statusCode();
            if (matchedRule.isEmpty() && statusCode >= 200 && statusCode < 400) {
                observedPathStore.record(path);
            }

            boolean bodyAllowed = statusCode != 204 && statusCode != 304
                    && !"HEAD".equalsIgnoreCase(method);
            try (InputStream upstreamBody = upstreamResponse.body()) {
                // Read the first chunk BEFORE committing the response status. If the upstream
                // fails immediately (e.g. the fault port closes the connection), the exception
                // is caught below while responseStarted is still false, so the client gets a
                // clean 502. Once the status is committed we can only stream; a failure after
                // this point yields a truncated body (unavoidable with unbuffered streaming).
                byte[] chunk = new byte[8192];
                int firstRead = bodyAllowed ? upstreamBody.read(chunk) : -1;

                upstreamResponse.headers().map().forEach((name, values) -> {
                    if (!isHopByHop(name)) {
                        values.forEach(v -> exchange.getResponseHeaders().add(name, v));
                    }
                });

                responseStarted = true;
                // Length 0 selects chunked transfer encoding, letting us stream an
                // unknown-length body without buffering it; -1 means "no body".
                exchange.sendResponseHeaders(statusCode, bodyAllowed && firstRead != -1 ? 0 : -1);
                if (bodyAllowed && firstRead != -1) {
                    try (OutputStream os = exchange.getResponseBody()) {
                        // Stream in fixed-size chunks: the proxy's memory stays bounded by the
                        // buffer regardless of body size, so a large or unbounded upstream
                        // response (e.g. the OversizedResponse fault) is forwarded faithfully.
                        os.write(chunk, 0, firstRead);
                        int read;
                        while ((read = upstreamBody.read(chunk)) != -1) {
                            os.write(chunk, 0, read);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (!responseStarted) {
                exchange.sendResponseHeaders(502, -1);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Proxy error for " + method + " " + path, e);
            if (!responseStarted) {
                exchange.sendResponseHeaders(502, -1);
            }
        } finally {
            exchange.close();
        }
    }

    private static boolean isHopByHop(String header) {
        return HOP_BY_HOP_HEADERS.contains(header.toLowerCase());
    }
}
