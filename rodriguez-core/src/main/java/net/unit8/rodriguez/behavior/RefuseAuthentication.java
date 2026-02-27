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
 * An HTTP behavior that returns an HTTP 401 Unauthorized response.
 *
 * <p>Responds with a configurable {@code WWW-Authenticate} header and JSON error body,
 * simulating an authentication failure.
 */
public class RefuseAuthentication implements HttpInstabilityBehavior, MetricsAvailable {

    /**
     * Creates a new {@code RefuseAuthentication} behavior instance.
     */
    public RefuseAuthentication() {
    }

    private int responseStatus = 401;
    private String wwwAuthenticate = "Bearer realm=\"rodriguez\"";
    private String responseBody = "{\"error\":\"unauthorized\",\"message\":\"Authentication credentials were refused\"}";
    private String contentType = "application/json";
    private int delay = 0;

    @Override
    public void handle(HttpExchange exchange) throws InterruptedException {
        try {
            if (delay > 0) {
                TimeUnit.MILLISECONDS.sleep(delay);
            }
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().put("Content-Type", List.of(contentType));
            exchange.getResponseHeaders().put("WWW-Authenticate", List.of(wwwAuthenticate));
            exchange.sendResponseHeaders(responseStatus, body.length);
            OutputStream os = exchange.getResponseBody();
            os.write(body);
            os.flush();
            getMetricRegistry().counter(MetricRegistry.name(RefuseAuthentication.class, "handle-complete")).inc();
        } catch (IOException e) {
            if (Objects.equals(e.getMessage(), "Broken pipe")) {
                getMetricRegistry().counter(MetricRegistry.name(RefuseAuthentication.class, "client-timeout")).inc();
            } else {
                getMetricRegistry().counter(MetricRegistry.name(RefuseAuthentication.class, "other-error")).inc();
            }
        } finally {
            exchange.close();
        }
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
     * Returns the {@code WWW-Authenticate} header value.
     *
     * @return the WWW-Authenticate header string
     */
    public String getWwwAuthenticate() {
        return wwwAuthenticate;
    }

    /**
     * Sets the {@code WWW-Authenticate} header value.
     *
     * @param wwwAuthenticate the WWW-Authenticate header string
     */
    public void setWwwAuthenticate(String wwwAuthenticate) {
        this.wwwAuthenticate = wwwAuthenticate;
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
