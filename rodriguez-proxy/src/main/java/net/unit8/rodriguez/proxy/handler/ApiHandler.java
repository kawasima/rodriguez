package net.unit8.rodriguez.proxy.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.unit8.rodriguez.proxy.ProxyConfig;
import net.unit8.rodriguez.proxy.model.BehaviorInfo;
import net.unit8.rodriguez.proxy.model.FaultRule;
import net.unit8.rodriguez.proxy.store.FaultRuleStore;
import net.unit8.rodriguez.proxy.store.ObservedPathStore;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST API handler for managing fault injection rules.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET    /_proxy/api/rules        — list active rules</li>
 *   <li>POST   /_proxy/api/rules        — create a rule (faultPort is optional; resolved from faultType)</li>
 *   <li>DELETE /_proxy/api/rules         — remove all rules</li>
 *   <li>DELETE /_proxy/api/rules/{id}    — remove a rule by ID</li>
 *   <li>GET    /_proxy/api/behaviors     — list available fault behaviors (from control API)</li>
 *   <li>GET    /_proxy/api/paths         — list observed paths (HTTP 200-399)</li>
 * </ul>
 */
public class ApiHandler implements HttpHandler {
    private static final Logger LOG = Logger.getLogger(ApiHandler.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();
    private final FaultRuleStore store;
    private final ProxyConfig config;
    private final ObservedPathStore observedPathStore;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, Integer> behaviorPortCache = new ConcurrentHashMap<>();
    private volatile Set<Integer> knownFaultPorts = Set.of();
    private volatile long behaviorCacheAt = 0L;
    private static final long BEHAVIOR_CACHE_TTL_MS = 60_000L;
    /** Upper bound on the per-rule request count, to reject absurd/negative values. */
    private static final int MAX_RULE_COUNT = 1_000_000;

    /**
     * Creates a new API handler.
     *
     * @param store             fault rule store
     * @param config            proxy configuration
     * @param observedPathStore store for observed paths
     */
    public ApiHandler(FaultRuleStore store, ProxyConfig config, ObservedPathStore observedPathStore) {
        this.store = store;
        this.config = config;
        this.observedPathStore = observedPathStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", config.getAllowedOrigin());
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String subPath = path.substring("/_proxy/api".length());

        try {
            switch (subPath) {
                case "/rules" -> {
                    if ("GET".equals(method)) {
                        handleListRules(exchange);
                    } else if ("POST".equals(method)) {
                        handleCreateRule(exchange);
                    } else if ("DELETE".equals(method)) {
                        handleClearAllRules(exchange);
                    } else {
                        exchange.sendResponseHeaders(405, -1);
                    }
                }
                case "/behaviors" -> {
                    if ("GET".equals(method)) {
                        handleListBehaviors(exchange);
                    } else {
                        exchange.sendResponseHeaders(405, -1);
                    }
                }
                case "/paths" -> {
                    if ("GET".equals(method)) {
                        handleListPaths(exchange);
                    } else {
                        exchange.sendResponseHeaders(405, -1);
                    }
                }
                default -> {
                    if (subPath.startsWith("/rules/") && "DELETE".equals(method)) {
                        String ruleId = subPath.substring("/rules/".length());
                        handleDeleteRule(exchange, ruleId);
                    } else if (subPath.matches("/rules/[^/]+/increment") && "PATCH".equals(method)) {
                        String ruleId = subPath.substring("/rules/".length(), subPath.lastIndexOf("/increment"));
                        handleIncrementRule(exchange, ruleId);
                    } else {
                        exchange.sendResponseHeaders(404, -1);
                    }
                }
            }
        } catch (Exception e) {
            // Log the detail server-side; never leak exception text to the client.
            LOG.log(Level.WARNING, "Error handling API request " + method + " " + path, e);
            byte[] error = mapper.writeValueAsBytes(Map.of("error", "Internal server error"));
            sendJson(exchange, 500, error);
        } finally {
            exchange.close();
        }
    }

    private void handleListRules(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> ruleList = store.listRules().stream()
                .map(this::ruleToMap)
                .toList();
        sendJson(exchange, 200, mapper.writeValueAsBytes(ruleList));
    }

    private void handleCreateRule(HttpExchange exchange) throws IOException {
        // Bound the request body so a large POST cannot OOM the control port.
        byte[] body = ProxyHandler.readBounded(
                exchange.getRequestBody(), config.getMaxRequestBodyBytes());
        if (body == null) {
            exchange.sendResponseHeaders(413, -1);
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> json = mapper.readValue(body, Map.class);

        String pathPattern = (String) json.get("pathPattern");
        String faultType = (String) json.get("faultType");

        if (pathPattern == null || pathPattern.isEmpty() || faultType == null || faultType.isEmpty()) {
            sendJson(exchange, 400, mapper.writeValueAsBytes(
                    Map.of("error", "pathPattern and faultType are required")));
            return;
        }

        if (pathPattern.length() > FaultRule.MAX_PATTERN_LENGTH) {
            sendJson(exchange, 400, mapper.writeValueAsBytes(Map.of(
                    "error", "pathPattern exceeds maximum length of " + FaultRule.MAX_PATTERN_LENGTH)));
            return;
        }

        int count = 1;
        if (json.containsKey("count")) {
            if (!(json.get("count") instanceof Number number)) {
                sendJson(exchange, 400, mapper.writeValueAsBytes(
                        Map.of("error", "count must be a number")));
                return;
            }
            count = number.intValue();
        }
        if (count < 1 || count > MAX_RULE_COUNT) {
            sendJson(exchange, 400, mapper.writeValueAsBytes(
                    Map.of("error", "count must be between 1 and " + MAX_RULE_COUNT)));
            return;
        }

        int faultPort;
        if (json.containsKey("faultPort")) {
            if (!(json.get("faultPort") instanceof Number number)) {
                sendJson(exchange, 400, mapper.writeValueAsBytes(
                        Map.of("error", "faultPort must be a number")));
                return;
            }
            faultPort = number.intValue();
            if (faultPort < 1 || faultPort > 65535) {
                sendJson(exchange, 400, mapper.writeValueAsBytes(
                        Map.of("error", "faultPort must be between 1 and 65535")));
                return;
            }
            // SSRF guard: only allow forwarding to legitimate Rodriguez fault ports,
            // never an arbitrary client-chosen localhost port.
            if (!isFaultPortAllowed(faultPort)) {
                sendJson(exchange, 400, mapper.writeValueAsBytes(Map.of(
                        "error", "faultPort " + faultPort + " is not an allowed Rodriguez fault port")));
                return;
            }
        } else {
            Integer resolved = resolveFaultPort(faultType);
            if (resolved == null) {
                sendJson(exchange, 400, mapper.writeValueAsBytes(
                        Map.of("error", "Unknown faultType: " + faultType)));
                return;
            }
            // Apply the same allow-list check as the explicit-faultPort branch, so a
            // configured allowedFaultPorts restriction cannot be bypassed via faultType.
            if (!isFaultPortAllowed(resolved)) {
                sendJson(exchange, 400, mapper.writeValueAsBytes(Map.of(
                        "error", "faultPort " + resolved + " is not an allowed Rodriguez fault port")));
                return;
            }
            faultPort = resolved;
        }

        String duration = (String) json.get("duration");
        FaultRule rule;
        try {
            rule = new FaultRule(pathPattern, faultType, faultPort, count, duration);
        } catch (java.util.regex.PatternSyntaxException e) {
            sendJson(exchange, 400, mapper.writeValueAsBytes(
                    Map.of("error", "Invalid pathPattern regex: " + e.getDescription())));
            return;
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, mapper.writeValueAsBytes(
                    Map.of("error", "Invalid duration: " + duration + " (use e.g. \"30s\", \"5m\", \"1h\")")));
            return;
        }
        store.addRule(rule);

        sendJson(exchange, 201, mapper.writeValueAsBytes(ruleToMap(rule)));
    }

    private void handleClearAllRules(HttpExchange exchange) throws IOException {
        store.clearAll();
        exchange.sendResponseHeaders(204, -1);
    }

    private void handleDeleteRule(HttpExchange exchange, String ruleId) throws IOException {
        store.removeRule(ruleId);
        exchange.sendResponseHeaders(204, -1);
    }

    private void handleIncrementRule(HttpExchange exchange, String ruleId) throws IOException {
        var rule = store.incrementRule(ruleId);
        if (rule.isEmpty()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        sendJson(exchange, 200, mapper.writeValueAsBytes(ruleToMap(rule.get())));
    }

    private void handleListBehaviors(HttpExchange exchange) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getControlUrl() + "/config"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());
            JsonNode ports = root.get("ports");

            List<BehaviorInfo> behaviors = new ArrayList<>();
            if (ports != null) {
                for (var entry : ports.properties()) {
                    int port = Integer.parseInt(entry.getKey());
                    String type = entry.getValue().path("type").asText();
                    behaviors.add(new BehaviorInfo(type, port, ""));
                }
            }
            sendJson(exchange, 200, mapper.writeValueAsBytes(behaviors));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching behaviors from control API", e);
        }
    }

    private void handleListPaths(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, mapper.writeValueAsBytes(observedPathStore.getPaths()));
    }

    private Integer resolveFaultPort(String faultType) {
        refreshBehaviorCacheIfStale();
        return behaviorPortCache.get(faultType);
    }

    /**
     * Returns whether the client-supplied fault port may be targeted.
     *
     * <p>If an explicit allow-list is configured it is authoritative; otherwise the
     * proxy falls back to the set of behavior ports advertised by the control API.
     * This blocks the proxy from being used as an arbitrary localhost-port relay.
     *
     * @param port the candidate fault port
     * @return true if the port is a legitimate Rodriguez fault port
     */
    private boolean isFaultPortAllowed(int port) {
        if (config.hasAllowedFaultPorts()) {
            return config.isFaultPortAllowed(port);
        }
        refreshBehaviorCacheIfStale();
        return knownFaultPorts.contains(port);
    }

    /**
     * Refreshes the cached view of the control API's advertised behavior ports
     * (both the faultType-to-port map and the set of known ports) when stale.
     */
    private synchronized void refreshBehaviorCacheIfStale() {
        if (!behaviorPortCache.isEmpty()
                && (System.currentTimeMillis() - behaviorCacheAt) < BEHAVIOR_CACHE_TTL_MS) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getControlUrl() + "/config"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode ports = mapper.readTree(response.body()).get("ports");
            if (ports != null) {
                Map<String, Integer> types = new HashMap<>();
                Set<Integer> portSet = new HashSet<>();
                for (var entry : ports.properties()) {
                    int port = Integer.parseInt(entry.getKey());
                    String type = entry.getValue().path("type").asText();
                    types.put(type, port);
                    portSet.add(port);
                }
                behaviorPortCache.putAll(types);
                knownFaultPorts = Set.copyOf(portSet);
                behaviorCacheAt = System.currentTimeMillis();
            }
        } catch (IOException | InterruptedException e) {
            LOG.log(Level.WARNING, "Failed to fetch behaviors from control API", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private Map<String, Object> ruleToMap(FaultRule rule) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rule.getId());
        map.put("pathPattern", rule.getPathPattern());
        map.put("faultType", rule.getFaultType());
        map.put("faultPort", rule.getFaultPort());
        map.put("remaining", rule.getRemaining());
        if (rule.getDuration() != null) {
            map.put("duration", rule.getDuration().toString());
        }
        return map;
    }

    private void sendJson(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
