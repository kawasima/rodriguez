package net.unit8.rodriguez.proxy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.unit8.rodriguez.proxy.ProxyConfig;
import net.unit8.rodriguez.proxy.model.FaultRule;
import net.unit8.rodriguez.proxy.store.FaultRuleStore;
import net.unit8.rodriguez.proxy.store.ObservedPathStore;

import java.io.IOException;
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

        try {
            String query = exchange.getRequestURI().getRawQuery();
            String targetUri = targetBase + path + (query != null ? "?" + query : "");

            String contentLengthHeader = exchange.getRequestHeaders().getFirst("Content-Length");
            if (contentLengthHeader != null) {
                long contentLength = Long.parseLong(contentLengthHeader);
                if (contentLength > config.getMaxRequestBodyBytes()) {
                    exchange.sendResponseHeaders(413, -1);
                    exchange.close();
                    return;
                }
            }
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            if (requestBody.length > config.getMaxRequestBodyBytes()) {
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

            HttpResponse<byte[]> upstreamResponse = httpClient.send(
                    reqBuilder.build(),
                    HttpResponse.BodyHandlers.ofByteArray());

            int statusCode = upstreamResponse.statusCode();
            if (matchedRule.isEmpty() && statusCode >= 200 && statusCode < 400) {
                observedPathStore.record(path);
            }

            upstreamResponse.headers().map().forEach((name, values) -> {
                if (!isHopByHop(name)) {
                    values.forEach(v -> exchange.getResponseHeaders().add(name, v));
                }
            });

            byte[] responseBody = upstreamResponse.body();
            exchange.sendResponseHeaders(statusCode,
                    responseBody != null && responseBody.length > 0 ? responseBody.length : -1);
            if (responseBody != null && responseBody.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBody);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exchange.sendResponseHeaders(502, -1);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Proxy error for " + method + " " + path, e);
            exchange.sendResponseHeaders(502, -1);
        } finally {
            exchange.close();
        }
    }

    private static boolean isHopByHop(String header) {
        return HOP_BY_HOP_HEADERS.contains(header.toLowerCase());
    }
}
