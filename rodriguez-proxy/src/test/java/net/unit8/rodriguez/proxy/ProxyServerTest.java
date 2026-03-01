package net.unit8.rodriguez.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import net.unit8.rodriguez.HarnessServer;
import net.unit8.rodriguez.behavior.SlowResponse;
import net.unit8.rodriguez.configuration.HarnessConfig;
import org.junit.jupiter.api.*;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProxyServerTest {
    static HarnessServer harnessServer;
    static ProxyServer proxyServer;
    static HttpServer upstreamServer;
    static ObjectMapper mapper;
    static HttpClient httpClient;

    static final int PROXY_PORT = 19220;
    static final int UPSTREAM_PORT = 19080;
    static final int FAULT_PORT = 19205;
    static final int CONTROL_PORT = 19200;

    @BeforeAll
    static void setUp() throws Exception {
        mapper = new ObjectMapper();
        httpClient = HttpClient.newHttpClient();

        // Start a lightweight upstream server
        upstreamServer = HttpServer.create(new InetSocketAddress(UPSTREAM_PORT), 0);
        upstreamServer.createContext("/api/hello", exchange -> {
            byte[] body = "{\"message\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
            exchange.close();
        });
        upstreamServer.createContext("/api/echo", exchange -> {
            byte[] reqBody = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, reqBody.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(reqBody);
            }
            exchange.close();
        });
        upstreamServer.start();

        // Start rodriguez harness (SlowResponse on fault port, control on CONTROL_PORT)
        HarnessConfig harnessConfig = new HarnessConfig();
        harnessConfig.setControlPort(CONTROL_PORT);
        harnessConfig.setPorts(Map.of(FAULT_PORT, new SlowResponse()));
        harnessServer = new HarnessServer(harnessConfig);
        harnessServer.start();

        // Start proxy server
        ProxyConfig config = new ProxyConfig();
        config.setPort(PROXY_PORT);
        config.setUpstream("http://localhost:" + UPSTREAM_PORT);
        config.setControlUrl("http://localhost:" + CONTROL_PORT);
        proxyServer = new ProxyServer(config);
        proxyServer.start();
    }

    @AfterAll
    static void tearDown() {
        if (proxyServer != null) proxyServer.shutdown();
        if (harnessServer != null) harnessServer.shutdown();
        if (upstreamServer != null) upstreamServer.stop(0);
    }

    // ---- Proxy forwarding ----

    @Test
    void forwardsRequestToUpstream() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/api/hello"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = mapper.readTree(response.body());
        assertThat(json.get("message").asText()).isEqualTo("hello");
    }

    @Test
    void forwardsPostBodyToUpstream() throws Exception {
        String requestBody = "{\"key\":\"value\"}";
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/api/echo"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(requestBody);
    }

    @Test
    void internalProxyPathReturns404() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/_proxy/nonexistent"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
    }

    // ---- API: behaviors ----

    @Test
    void listBehaviorsReturnsKnownFaults() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/_proxy/api/behaviors"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = mapper.readTree(response.body());
        assertThat(json.isArray()).isTrue();
        assertThat(json.size()).isGreaterThan(0);
        boolean hasSlowResponse = false;
        for (JsonNode node : json) {
            if ("SlowResponse".equals(node.get("name").asText())) {
                hasSlowResponse = true;
            }
        }
        assertThat(hasSlowResponse).isTrue();
    }

    // ---- API: rules CRUD ----

    @Test
    void createAndListRule() throws Exception {
        String ruleBody = mapper.writeValueAsString(Map.of(
                "pathPattern", "/api/test",
                "faultType", "SlowResponse",
                "faultPort", FAULT_PORT,
                "count", 1));

        HttpResponse<String> createResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/_proxy/api/rules"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(ruleBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(createResponse.statusCode()).isEqualTo(201);
        JsonNode created = mapper.readTree(createResponse.body());
        assertThat(created.get("pathPattern").asText()).isEqualTo("/api/test");
        assertThat(created.get("faultType").asText()).isEqualTo("SlowResponse");
        String ruleId = created.get("id").asText();

        // List rules — must contain the created rule
        HttpResponse<String> listResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/_proxy/api/rules"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(listResponse.statusCode()).isEqualTo(200);
        JsonNode rules = mapper.readTree(listResponse.body());
        assertThat(rules.isArray()).isTrue();
        boolean found = false;
        for (JsonNode rule : rules) {
            if (ruleId.equals(rule.get("id").asText())) {
                found = true;
            }
        }
        assertThat(found).isTrue();

        // Cleanup
        httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/_proxy/api/rules/" + ruleId))
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void deleteRule() throws Exception {
        String ruleBody = mapper.writeValueAsString(Map.of(
                "pathPattern", "/api/to-delete",
                "faultType", "SlowResponse",
                "faultPort", FAULT_PORT,
                "count", 5));

        HttpResponse<String> createResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/_proxy/api/rules"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(ruleBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        String ruleId = mapper.readTree(createResponse.body()).get("id").asText();

        HttpResponse<String> deleteResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/_proxy/api/rules/" + ruleId))
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(deleteResponse.statusCode()).isEqualTo(204);

        // Verify rule is removed from the list
        HttpResponse<String> listResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/_proxy/api/rules"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        JsonNode rules = mapper.readTree(listResponse.body());
        for (JsonNode rule : rules) {
            assertThat(rule.get("id").asText()).isNotEqualTo(ruleId);
        }
    }

    @Test
    void corsHeadersPresent() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/_proxy/api/behaviors"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.headers().firstValue("access-control-allow-origin"))
                .hasValue("*");
    }

    @Test
    void optionsPreflight() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + PROXY_PORT + "/_proxy/api/rules"))
                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(204);
    }
}
