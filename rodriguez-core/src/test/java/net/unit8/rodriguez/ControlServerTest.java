package net.unit8.rodriguez;

import net.unit8.rodriguez.behavior.SlowResponse;
import net.unit8.rodriguez.configuration.HarnessConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ControlServerTest {
    static HarnessServer server;
    static OkHttpClient http;
    static final int CONTROL_PORT = 19200;

    @BeforeAll
    static void setUp() {
        http = new OkHttpClient();

        HarnessConfig config = new HarnessConfig();
        config.setControlPort(CONTROL_PORT);
        config.setPorts(Map.of(19201, new SlowResponse()));
        server = new HarnessServer(config);
        server.start();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) server.shutdown();
    }

    @Test
    void configEndpointReturnsJson() throws Exception {
        Response response = http.newCall(new Request.Builder()
                .url("http://localhost:" + CONTROL_PORT + "/config")
                .get()
                .build()).execute();

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.header("Content-Type")).contains("application/json");
        String body = response.body().string();
        assertThat(body).isNotBlank();
    }

    @Test
    void metricsEndpointReturnsJson() throws Exception {
        Response response = http.newCall(new Request.Builder()
                .url("http://localhost:" + CONTROL_PORT + "/metrics")
                .get()
                .build()).execute();

        assertThat(response.code()).isEqualTo(200);
        assertThat(response.header("Content-Type")).contains("application/json");
    }

    @Test
    void unknownPathReturns404() throws Exception {
        Response response = http.newCall(new Request.Builder()
                .url("http://localhost:" + CONTROL_PORT + "/unknown")
                .get()
                .build()).execute();

        assertThat(response.code()).isEqualTo(404);
    }

    @Test
    void wrongMethodReturns404() throws Exception {
        // POST /config is not defined → 404
        Response response = http.newCall(new Request.Builder()
                .url("http://localhost:" + CONTROL_PORT + "/config")
                .post(okhttp3.RequestBody.create(new byte[0]))
                .build()).execute();

        assertThat(response.code()).isEqualTo(404);
    }
}
