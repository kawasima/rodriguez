package net.unit8.rodriguez.behavior;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityBehavior;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * An HTTP behavior that sends a response body one byte at a time with a configurable delay.
 *
 * <p>Simulates an extremely slow server by writing a single byte per interval,
 * which can trigger client-side read timeouts.
 */
public class SlowResponse implements HttpInstabilityBehavior, MetricsAvailable {

    /**
     * Creates a new {@code SlowResponse} behavior instance.
     */
    public SlowResponse() {
    }

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

    /**
     * Returns the interval in milliseconds between each byte sent.
     *
     * @return the interval in milliseconds
     */
    public long getInterval() {
        return interval;
    }

    /**
     * Sets the interval in milliseconds between each byte sent.
     *
     * @param interval the interval in milliseconds
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }

    /**
     * Returns the byte value sent repeatedly in the response.
     *
     * @return the byte value to send
     */
    public byte getSentChar() {
        return sentChar;
    }

    /**
     * Sets the byte value sent repeatedly in the response.
     *
     * @param sentChar the byte value to send
     */
    public void setSentChar(byte sentChar) {
        this.sentChar = sentChar;
    }
}
