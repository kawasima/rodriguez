package net.unit8.rodriguez.gcp.behavior;

import com.sun.net.httpserver.HttpServer;
import net.unit8.rodriguez.metrics.MetricRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GCSMock}, exercising the HTTP surface through a real
 * {@link HttpServer} so the request-body handling paths are covered end-to-end.
 */
class GCSMockTest {
    private HttpServer server;
    private MetricRegistry metricRegistry;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void start(GCSMock mock) {
        metricRegistry = new MetricRegistry();
        mock.setMetricRegistry(metricRegistry);
        server.createContext("/", mock::handle);
        server.start();
    }

    @Test
    void oversizedCreateBucketBodyIsRejectedWith413() throws Exception {
        // Cap the metadata body at 16 bytes so a modest chunked body trips the limit.
        start(new GCSMock(16));

        // ofInputStream reports an unknown length, so the request is sent chunked
        // (no Content-Length) -- exactly the case the bounded read must guard.
        byte[] oversized = new byte[64];
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/storage/v1/b?project=test"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(oversized)))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("maximum allowed size");
        assertThat(metricRegistry.counter(MetricRegistry.name(GCSMock.class, "client-error")).getCount())
                .isEqualTo(1);
    }

    @Test
    void smallCreateBucketBodyIsParsedAndSucceeds() throws Exception {
        start(new GCSMock());

        String body = "{\"name\":\"my-bucket\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + "/storage/v1/b?project=test"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"name\":\"my-bucket\"");
    }
}
