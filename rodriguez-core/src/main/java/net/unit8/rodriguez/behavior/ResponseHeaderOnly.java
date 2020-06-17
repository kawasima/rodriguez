package net.unit8.rodriguez.behavior;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityBehavior;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.util.Objects;

public class ResponseHeaderOnly implements HttpInstabilityBehavior, MetricsAvailable {
    @Override
    public void handle(HttpExchange exchange) throws InterruptedException {
        try {
            exchange.sendResponseHeaders(200, 5000);
            getMetricRegistry().counter(MetricRegistry.name(ResponseHeaderOnly.class, "handle-complete")).inc();
        } catch (IOException e) {
            if (Objects.equals(e.getMessage(), "Broken pipe")) {
                getMetricRegistry().counter(MetricRegistry.name(ResponseHeaderOnly.class, "client-timeout")).inc();
            } else {
                getMetricRegistry().counter(MetricRegistry.name(ResponseHeaderOnly.class, "other-error")).inc();
            }
        }

    }
}
