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
import java.util.*;
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
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
            byte[] error = mapper.writeValueAsBytes(Map.of("error", e.getMessage()));
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
        byte[] body = exchange.getRequestBody().readAllBytes();
        @SuppressWarnings("unchecked")
        Map<String, Object> json = mapper.readValue(body, Map.class);

        String pathPattern = (String) json.get("pathPattern");
        String faultType = (String) json.get("faultType");
        int count = json.containsKey("count") ? ((Number) json.get("count")).intValue() : 1;

        int faultPort;
        if (json.containsKey("faultPort")) {
            faultPort = ((Number) json.get("faultPort")).intValue();
        } else {
            Integer resolved = resolveFaultPort(faultType);
            if (resolved == null) {
                sendJson(exchange, 400, mapper.writeValueAsBytes(
                        Map.of("error", "Unknown faultType: " + faultType)));
                return;
            }
            faultPort = resolved;
        }

        String duration = (String) json.get("duration");
        FaultRule rule = new FaultRule(pathPattern, faultType, faultPort, count, duration);
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
                ports.fields().forEachRemaining(entry -> {
                    int port = Integer.parseInt(entry.getKey());
                    String type = entry.getValue().path("type").asText();
                    behaviors.add(new BehaviorInfo(type, port, ""));
                });
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
        Integer cached = behaviorPortCache.get(faultType);
        if (cached != null) {
            return cached;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getControlUrl() + "/config"))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode ports = mapper.readTree(response.body()).get("ports");
            if (ports != null) {
                ports.fields().forEachRemaining(entry -> {
                    String type = entry.getValue().path("type").asText();
                    behaviorPortCache.put(type, Integer.parseInt(entry.getKey()));
                });
            }
        } catch (IOException | InterruptedException e) {
            LOG.log(Level.WARNING, "Failed to fetch behaviors from control API", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return behaviorPortCache.get(faultType);
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
