package net.unit8.rodriguez;

import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import dev.failsafe.Timeout;
import dev.failsafe.TimeoutExceededException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Objects;

public class FailsafeClientTest {
    final RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
            .withMaxRetries(3)
            .build();

    final Timeout<Object> timeout = Timeout.builder(Duration.ofSeconds(15))
            .withInterrupt()
            .build();

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
    void connectionRefused() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(1000))
                .readTimeout(Duration.ofMillis(3000))
                .build();
        Request request = new Request.Builder()
                .url("http://localhost:10201/")
                .get()
                .build();
        Assertions.assertThatThrownBy(() -> Failsafe.with(timeout, retryPolicy)
                .get(() -> client.newCall(request).execute())).hasCauseInstanceOf(ConnectException.class);
    }

    @Test
    void notAccept() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(1000))
                .readTimeout(Duration.ofMillis(3000))
                .build();
        Request request = new Request.Builder()
                .url("http://localhost:10202/")
                .get()
                .build();
        Assertions.assertThatThrownBy(() -> Failsafe.with(timeout, retryPolicy)
                .get(() -> client.newCall(request).execute())).hasCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    void rstPacket() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(1000))
                .build();
        Request request = new Request.Builder()
                .url("http://localhost:10203/")
                .get()
                .build();
        Assertions.assertThatThrownBy(() -> client.newCall(request).execute()).isInstanceOf(SocketException.class)
                .hasMessageContaining("Connection reset");
    }

    /**
     * Retry 3 times. And SocketTimeoutException by the client side.
     */
    @Test
    void neverDrain() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(1000))
                .readTimeout(Duration.ofMillis(3000))
                .build();
        Request request = new Request.Builder()
                .url("http://localhost:10204/")
                .get()
                .build();
        Assertions.assertThatThrownBy(() -> {
            Response response = Failsafe.with(timeout, retryPolicy)
                    .get(() -> client.newCall(request).execute());
        }).hasCauseInstanceOf(SocketTimeoutException.class);
    }

    @Test
    void slowResponse() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(1000))
                .readTimeout(Duration.ofMillis(3000))
                .build();
        Request request = new Request.Builder()
                .url("http://localhost:10205/")
                .get()
                .build();
        Response response = Failsafe.with(timeout, retryPolicy)
                .get(() -> client.newCall(request).execute());
        Assertions.assertThatThrownBy(() -> Failsafe.with(timeout)
                .run(() -> {
            char[] cbuf = new char[1024];
            Reader reader = Objects.requireNonNull(response.body()).charStream();
            @SuppressWarnings("MismatchedQueryAndUpdateOfStringBuilder")
            StringBuilder sb = new StringBuilder();
            while(!Thread.interrupted()) {
                int read = reader.read(cbuf, 0, cbuf.length);
                if (read <= 0) break;
                sb.append(cbuf, 0, read);
            }
        })).isInstanceOf(TimeoutExceededException.class);
    }
    /**
     * Response has returns once time, but blocking read cause a timeout.
     */
    @Test
    void responseHeaderOnly() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofMillis(1000))
                .readTimeout(Duration.ofMillis(3000))
                .build();
        Request request = new Request.Builder()
                .url("http://localhost:10207/")
                .get()
                .build();
        Response response = Failsafe.with(timeout, retryPolicy)
                .get(() -> client.newCall(request).execute());
        Assertions.assertThatThrownBy(() -> {
            char[] cbuf = new char[1024];
            Reader reader = Objects.requireNonNull(response.body()).charStream();
            @SuppressWarnings("MismatchedQueryAndUpdateOfStringBuilder")
            StringBuilder sb = new StringBuilder();
            while(true) {
                int read = reader.read(cbuf, 0, cbuf.length);
                if (read <= 0) break;
                sb.append(cbuf, 0, read);
            }
        }).isInstanceOf(SocketTimeoutException.class);
    }


}
