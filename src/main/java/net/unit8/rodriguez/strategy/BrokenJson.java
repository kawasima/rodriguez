package net.unit8.rodriguez.strategy;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityStrategy;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class BrokenJson implements HttpInstabilityStrategy, MetricsAvailable {
    int delay = 0;

    @Override
    public void handle(HttpExchange exchange) throws InterruptedException {
        String responseBody = "{";
        try {
            TimeUnit.MILLISECONDS.sleep(delay);
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().put("Content-Type", List.of("application/json"));
            exchange.sendResponseHeaders(200, body.length);
            OutputStream os = exchange.getResponseBody();
            os.write(body);
            os.flush();
            getMetricRegistry().counter(MetricRegistry.name(BrokenJson.class, "handle-complete")).inc();
        } catch (IOException e) {
            if (Objects.equals(e.getMessage(), "Broken pipe")) {
                getMetricRegistry().counter(MetricRegistry.name(BrokenJson.class, "client-timeout")).inc();
            } else {
                getMetricRegistry().counter(MetricRegistry.name(BrokenJson.class, "other-error")).inc();
            }
        } finally {
            exchange.close();
        }
    }
}
