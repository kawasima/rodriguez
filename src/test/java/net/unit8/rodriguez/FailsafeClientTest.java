package net.unit8.rodriguez;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.Timeout;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.Objects;

public class FailsafeClientTest {
    RetryPolicy<Object> retryPolicy = new RetryPolicy<>()
            .withMaxRetries(3);

    Timeout<Object> timeout = Timeout.of(Duration.ofSeconds(30));

    static HarnessServer server;

    @BeforeAll
    static void setup() {
        server = new HarnessServer();
        server.start();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    void test() throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(1000))
                .readTimeout(Duration.ofMillis(3000))
                .build();
        Request request = new Request.Builder()
                .url("http://localhost:10204/")
                .get()
                .build();
        Response response = Failsafe.with(timeout, retryPolicy)
                .get(() -> client.newCall(request).execute());
        char[] cbuf = new char[1024];
        Reader reader = Objects.requireNonNull(response.body()).charStream();
        StringBuilder sb = new StringBuilder();
        while(true) {
            int read = reader.read(cbuf, 0, cbuf.length);
            if (read <= 0) break;
            sb.append(cbuf, 0, read);
        }
        System.out.println(new String(cbuf));
    }

    @Test
    void neverDrain() throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(1000))
                .readTimeout(Duration.ofMillis(3000))
                .build();
        Request request = new Request.Builder()
                .url("http://localhost:10203/")
                .get()
                .build();
        Response response = Failsafe.with(timeout, retryPolicy)
                .get(() -> client.newCall(request).execute());
        char[] cbuf = new char[1024];
        Reader reader = Objects.requireNonNull(response.body()).charStream();
        StringBuilder sb = new StringBuilder();
        while(true) {
            int read = reader.read(cbuf, 0, cbuf.length);
            if (read <= 0) break;
            sb.append(cbuf, 0, read);
        }
        System.out.println(new String(cbuf));
    }

}
