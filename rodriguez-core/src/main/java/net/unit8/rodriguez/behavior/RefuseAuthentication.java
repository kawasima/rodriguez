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

public class RefuseAuthentication implements HttpInstabilityBehavior, MetricsAvailable {
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

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getWwwAuthenticate() {
        return wwwAuthenticate;
    }

    public void setWwwAuthenticate(String wwwAuthenticate) {
        this.wwwAuthenticate = wwwAuthenticate;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}
