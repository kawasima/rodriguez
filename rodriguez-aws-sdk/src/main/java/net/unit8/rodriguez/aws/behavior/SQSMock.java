package net.unit8.rodriguez.aws.behavior;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityBehavior;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.RequestParams;
import net.unit8.rodriguez.aws.behavior.sqs.SQSAction;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Mock implementation of Amazon SQS as an HTTP instability behavior.
 *
 * <p>Supports basic SQS operations (create/delete queue, send/receive/delete message,
 * get queue URL) using in-memory storage. Both AWS Query and JSON protocols are supported.</p>
 */
public class SQSMock implements HttpInstabilityBehavior, MetricsAvailable {
    private static final Logger LOG = Logger.getLogger(SQSMock.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Constructs an SQSMock instance.
     */
    public SQSMock() {
    }

    private String resolveActionName(HttpExchange exchange, RequestParams params) {
        // AWS JSON protocol: X-Amz-Target header (e.g. "AmazonSQS.GetQueueUrl")
        String target = exchange.getRequestHeaders().getFirst("X-Amz-Target");
        if (target != null && target.startsWith("AmazonSQS.")) {
            return target.substring("AmazonSQS.".length());
        }
        // AWS Query protocol: Action parameter in query string or form body
        String action = params.getFirst("Action");
        if (action != null) {
            return action;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(HttpExchange exchange) {
        try {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            boolean isJsonProtocol = contentType != null && contentType.contains("application/x-amz-json");

            final RequestParams params;
            if (isJsonProtocol) {
                params = new RequestParams();
                byte[] body = exchange.getRequestBody().readAllBytes();
                if (body.length > 0) {
                    Map<String, Object> json = mapper.readValue(body, Map.class);
                    json.forEach((k, v) -> {
                        if (v != null) params.put(k, v.toString());
                    });
                }
            } else {
                params = AWSRequest.of(exchange).getParams();
            }

            String actionName = resolveActionName(exchange, params);
            AWSRequest request = AWSRequest.of(exchange, params);
            Optional<SQSAction> action = Stream.of(SQSAction.values())
                    .filter(act -> act.name().equals(actionName))
                    .findAny();

            action.ifPresentOrElse(
                    act -> {
                        try {
                            Object result = act.handle(request);
                            if (result == null) {
                                exchange.sendResponseHeaders(200, -1);
                            } else {
                                exchange.getResponseHeaders().set("Content-Type", "application/x-amz-json-1.0");
                                exchange.sendResponseHeaders(200, 0);
                                LOG.fine(mapper.writeValueAsString(result));
                                mapper.writeValue(exchange.getResponseBody(), result);
                            }
                        } catch (IOException e) {
                            sendError(exchange, 500);
                        }
                    },
                    () -> sendError(exchange, 404)
            );
        } catch (Exception e) {
            getMetricRegistry().counter(MetricRegistry.name(SQSMock.class, "other-error"));
        }
    }

    private void sendError(HttpExchange exchange, int status) {
        try {
            exchange.sendResponseHeaders(status, -1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
