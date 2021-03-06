package net.unit8.rodriguez.behavior;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityBehavior;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SlowResponse implements HttpInstabilityBehavior, MetricsAvailable {
    private long interval = 3000;
    private byte sentChar = 0x20;

    @Override
    public void handle(HttpExchange exchange) throws InterruptedException {
        try {
            exchange.sendResponseHeaders(200, 5000);
            OutputStream os = exchange.getResponseBody();
            for (int i = 0; i < 5000; i++) {
                os.write(sentChar);
                os.flush();
                TimeUnit.MILLISECONDS.sleep(interval);
            }
            getMetricRegistry().counter(MetricRegistry.name(SlowResponse.class, "handle-complete")).inc();
        } catch (IOException e) {
            if (Objects.equals(e.getMessage(), "Broken pipe")) {
                getMetricRegistry().counter(MetricRegistry.name(SlowResponse.class, "client-timeout")).inc();
            } else {
                getMetricRegistry().counter(MetricRegistry.name(SlowResponse.class, "other-error")).inc();
            }
        } finally {
            exchange.close();
        }
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public byte getSentChar() {
        return sentChar;
    }

    public void setSentChar(byte sentChar) {
        this.sentChar = sentChar;
    }
}
