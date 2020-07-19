package net.unit8.rodriguez.aws.behavior;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityBehavior;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.behavior.sqs.SQSAction;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SQSMock implements HttpInstabilityBehavior, MetricsAvailable {
    private static final Logger LOG = Logger.getLogger(SQSMock.class.getName());
    private final XmlMapper mapper;

    public SQSMock() {
        mapper = XmlMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE)
                .addModule(new JavaTimeModule()
                        .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("YYYY-MM-dd'T'hh:mm:ss.s'Z'"))))
                .build();
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            AWSRequest request = AWSRequest.of(exchange);
            Optional<SQSAction> action = Stream.of(SQSAction.values())
                    .filter(act -> act.name().equals(request.getParams().getFirst("Action")))
                    .findAny();
            action.ifPresentOrElse(
                    act -> {
                        try {
                            exchange.sendResponseHeaders(200, 0);
                            Object result = act.handle(request);
                            LOG.fine(mapper.writeValueAsString(result));
                            mapper.writeValue(exchange.getResponseBody(), result);
                        } catch (IOException e) {
                            sendError(exchange, 500);
                        }
                    },
                    () -> sendError(exchange, 404)
            );
        } catch (RuntimeException e) {
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
