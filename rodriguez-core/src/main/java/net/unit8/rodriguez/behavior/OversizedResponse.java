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

/**
 * An HTTP behavior that returns a response body exceeding the expected size.
 *
 * <p>Responds with a large payload (default 10 MB) of repeated bytes, simulating
 * an oversized or unexpectedly large response that may cause client-side issues.
 */
public class OversizedResponse implements HttpInstabilityBehavior, MetricsAvailable {

    /**
     * Creates a new {@code OversizedResponse} behavior instance.
     */
    public OversizedResponse() {
    }

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

    /**
     * Returns the total response size in bytes.
     *
     * @return the response size in bytes
     */
    public long getResponseSize() {
        return responseSize;
    }

    /**
     * Sets the total response size in bytes.
     *
     * @param responseSize the response size in bytes
     */
    public void setResponseSize(long responseSize) {
        this.responseSize = responseSize;
    }

    /**
     * Returns the content type header value.
     *
     * @return the content type string
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the content type header value.
     *
     * @param contentType the content type string
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Returns the chunk size used for writing the response.
     *
     * @return the chunk size in bytes
     */
    public int getChunkSize() {
        return chunkSize;
    }

    /**
     * Sets the chunk size used for writing the response.
     *
     * @param chunkSize the chunk size in bytes
     */
    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
}
