package net.unit8.rodriguez.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.unit8.rodriguez.HarnessServer;
import net.unit8.rodriguez.behavior.SlowResponse;
import net.unit8.rodriguez.configuration.HarnessConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a configured {@code allowedFaultPorts} allow-list is enforced on BOTH
 * rule-creation branches: the explicit {@code faultPort} branch and the {@code faultType}
 * resolution branch. A client must not be able to bypass the allow-list by omitting
 * {@code faultPort} and relying on {@code faultType} resolution.
 */
class ApiHandlerAllowlistTest {
    static HarnessServer harnessServer;
    static ObjectMapper mapper;
    static HttpClient httpClient;

    static final int UPSTREAM_UNUSED = 0;
    static final int FAULT_PORT = 19305;
    static final int CONTROL_PORT = 19300;

    @BeforeAll
    static void setUp() throws Exception {
        mapper = new ObjectMapper();
        httpClient = HttpClient.newHttpClient();

        HarnessConfig harnessConfig = new HarnessConfig();
        harnessConfig.setControlPort(CONTROL_PORT);
        harnessConfig.setPorts(Map.of(FAULT_PORT, new SlowResponse()));
        harnessServer = new HarnessServer(harnessConfig);
        harnessServer.start();
    }

    @AfterAll
    static void tearDown() {
        if (harnessServer != null) harnessServer.shutdown();
    }

    private ProxyServer startProxy(int port, List<Integer> allowedFaultPorts) {
        ProxyConfig config = new ProxyConfig();
        config.setPort(port);
        config.setUpstream("http://localhost:" + UPSTREAM_UNUSED);
        config.setControlUrl("http://localhost:" + CONTROL_PORT);
        config.setAllowedOrigin("http://localhost:" + port);
        config.setAllowedFaultPorts(allowedFaultPorts);
        ProxyServer server = new ProxyServer(config);
        server.start();
        return server;
    }

    private HttpResponse<String> createFaultTypeOnlyRule(int proxyPort) throws Exception {
        String ruleBody = mapper.writeValueAsString(Map.of(
                "pathPattern", "/api/allowlist",
                "faultType", "SlowResponse",
                "count", 1));
        return httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + proxyPort + "/_proxy/api/rules"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(ruleBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void faultTypeResolvingToDisallowedPortReturns400() throws Exception {
        // The fault port SlowResponse resolves to (FAULT_PORT) is NOT in the allow-list,
        // so a faultType-only request must be rejected instead of silently bypassing it.
        ProxyServer proxy = startProxy(19321, List.of(29999));
        try {
            HttpResponse<String> response = createFaultTypeOnlyRule(19321);
            assertThat(response.statusCode()).isEqualTo(400);
            JsonNode json = mapper.readTree(response.body());
            assertThat(json.get("error").asText()).contains("not an allowed");
        } finally {
            proxy.shutdown();
        }
    }

    @Test
    void faultTypeResolvingToAllowedPortSucceeds() throws Exception {
        // When the resolved port IS allow-listed, faultType-only creation still works.
        ProxyServer proxy = startProxy(19322, List.of(FAULT_PORT));
        try {
            HttpResponse<String> response = createFaultTypeOnlyRule(19322);
            assertThat(response.statusCode()).isEqualTo(201);
            JsonNode json = mapper.readTree(response.body());
            assertThat(json.get("faultPort").asInt()).isEqualTo(FAULT_PORT);
        } finally {
            proxy.shutdown();
        }
    }
}
