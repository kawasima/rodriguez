package net.unit8.rodriguez.behavior;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityBehavior;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class OversizedResponse implements HttpInstabilityBehavior, MetricsAvailable {
    private long responseSize = 10_485_760; // 10 MB
    private String contentType = "application/octet-stream";
    private int chunkSize = 8192;

    @Override
    public void handle(HttpExchange exchange) throws InterruptedException {
        try {
            byte[] chunk = new byte[chunkSize];
            Arrays.fill(chunk, (byte) 0x41); // Fill with 'A'

            exchange.getResponseHeaders().put("Content-Type", List.of(contentType));
            exchange.sendResponseHeaders(200, responseSize);
            OutputStream os = exchange.getResponseBody();

            long remaining = responseSize;
            while (remaining > 0) {
                int toWrite = (int) Math.min(remaining, chunkSize);
                os.write(chunk, 0, toWrite);
                os.flush();
                remaining -= toWrite;
            }
            getMetricRegistry().counter(MetricRegistry.name(OversizedResponse.class, "handle-complete")).inc();
        } catch (IOException e) {
            if (Objects.equals(e.getMessage(), "Broken pipe")) {
                getMetricRegistry().counter(MetricRegistry.name(OversizedResponse.class, "client-timeout")).inc();
            } else {
                getMetricRegistry().counter(MetricRegistry.name(OversizedResponse.class, "other-error")).inc();
            }
        } finally {
            exchange.close();
        }
    }

    public long getResponseSize() {
        return responseSize;
    }

    public void setResponseSize(long responseSize) {
        this.responseSize = responseSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
}
