package net.unit8.rodriguez.behavior;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityBehavior;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * An HTTP behavior that returns a response with a mismatched content type.
 *
 * <p>By default, the {@code Content-Type} header claims {@code application/json} but the
 * actual response body is HTML, simulating a content type mismatch error.
 */
public class ContentTypeMismatch implements HttpInstabilityBehavior, MetricsAvailable {

    /**
     * Creates a new {@code ContentTypeMismatch} behavior instance.
     */
    public ContentTypeMismatch() {
    }

    private String contentType = "application/json";
    private String responseBody = "<html><body>unknown error</body></html>";
    private int responseStatus = 400;
    private int delay = 1000;

    @Override
    public void handle(HttpExchange exchange) throws InterruptedException {
        try {
            TimeUnit.MILLISECONDS.sleep(delay);
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().put("Content-Type", List.of(contentType));
            exchange.sendResponseHeaders(responseStatus, body.length);
            OutputStream os = exchange.getResponseBody();
            os.write(body);
            os.flush();
            getMetricRegistry().counter(MetricRegistry.name(ContentTypeMismatch.class, "handle-complete")).inc();
        } catch (IOException e) {
            if (Objects.equals(e.getMessage(), "Broken pipe")) {
                getMetricRegistry().counter(MetricRegistry.name(ContentTypeMismatch.class, "client-timeout")).inc();
            } else {
                getMetricRegistry().counter(MetricRegistry.name(ContentTypeMismatch.class, "other-error")).inc();
            }
        } finally {
            exchange.close();
        }
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
     * Returns the response body.
     *
     * @return the response body string
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Sets the response body.
     *
     * @param responseBody the response body string
     */
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    /**
     * Returns the HTTP response status code.
     *
     * @return the response status code
     */
    public int getResponseStatus() {
        return responseStatus;
    }

    /**
     * Sets the HTTP response status code.
     *
     * @param responseStatus the response status code
     */
    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    /**
     * Returns the delay in milliseconds before sending the response.
     *
     * @return the delay in milliseconds
     */
    public int getDelay() {
        return delay;
    }

    /**
     * Sets the delay in milliseconds before sending the response.
     *
     * @param delay the delay in milliseconds
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }
}
