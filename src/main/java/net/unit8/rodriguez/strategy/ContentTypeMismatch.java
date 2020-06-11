package net.unit8.rodriguez.strategy;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityStrategy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ContentTypeMismatch implements HttpInstabilityStrategy {
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
        } catch (IOException e) {
            if (Objects.equals(e.getMessage(), "Broken pipe")) {

            } else {

            }
        }
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }
}
