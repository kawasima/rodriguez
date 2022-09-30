package net.unit8.rodriguez.aws.behavior;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityBehavior;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.ErrorResponse;
import net.unit8.rodriguez.aws.behavior.s3.S3Action;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class S3Mock implements HttpInstabilityBehavior, MetricsAvailable {
    private static final Logger LOG = Logger.getLogger(S3Mock.class.getName());

    private static final DateTimeFormatter HTTP_DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
    private final XmlMapper mapper;
    private File s3Directory;
    private String endpointHost = "localhost";

    public S3Mock() {
        mapper = XmlMapper.builder()
                .addModule(new JavaTimeModule()
                        .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss.s'Z'"))))
                .build();
    }

    private String getBucketNameFromPath(HttpExchange exchange) {
        return Optional.ofNullable(exchange.getRequestURI().getPath())
                .filter(path -> !path.isEmpty() && !"/".equals(path))
                .map(path -> path.substring(1))
                .map(path -> path.contains("/") ? path.substring(0, path.indexOf('/')) : path)
                .orElseGet(() -> getBucketNameFromHost(exchange));
    }

    private String getBucketNameFromHost(HttpExchange exchange) {
        return Optional.ofNullable(exchange.getRequestHeaders().getFirst("Host"))
                .map(host -> {
                    Pattern bucketPattern = Pattern.compile("([a-z\\-]+)\\." + endpointHost);
                    return bucketPattern.matcher(host);
                })
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .orElse("");
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            AWSRequest request = AWSRequest.of(exchange);
            String bucketName = getBucketNameFromPath(exchange);
            boolean isPathStyleAccess = bucketName != null;
            if (!isPathStyleAccess) {
                bucketName = getBucketNameFromHost(exchange);
            }

            if (!bucketName.isEmpty()) {
                request.getParams().put("BucketName", bucketName);
            }

            String bn = bucketName;
            Optional.ofNullable(exchange.getRequestURI().getPath())
                    .map(path -> bn.isEmpty() ? ""
                            :
                            path.startsWith("/" + bn) ? path.substring(bn.length() + 1) : path)
                    .filter(path -> !path.isEmpty() && !path.equals("/"))
                    .map(path -> path.substring(1))
                    .ifPresent(objName -> request.getParams().put("ObjectName", objName));

            S3Action action = Stream.of(S3Action.values())
                    .filter(act -> act.isApplicable(request))
                    .findAny()
                    .orElse(S3Action.NotFound);

            Object response = action.handle(request);
            if (response instanceof ErrorResponse) {
                ((ErrorResponse) response).handle(exchange);
            } else if (response == null) {
                exchange.getResponseHeaders().set("Content-Length", "0");
                exchange.sendResponseHeaders(204, -1);
            } else if (response instanceof File) {
                File f = (File) response;
                exchange.sendResponseHeaders(200, f.length());
                byte[] buffer = new byte[4096];
                try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
                    while (true) {
                        int read = in.read(buffer);
                        if (read <= 0) {
                            break;
                        }
                        exchange.getResponseBody().write(buffer, 0, read);
                    }
                }
            } else {
                exchange.sendResponseHeaders(200, 0);
                LOG.fine(mapper.writeValueAsString(response));
                mapper.writeValue(exchange.getResponseBody(), response);
            }
        } catch (Exception e) {
            getMetricRegistry().counter(MetricRegistry.name(S3Mock.class, "other-error"));
            try {
                exchange.sendResponseHeaders(500, 0);
            } catch(IOException ignore) {

            }
        } finally {
            exchange.close();
        }
    }

    public void setS3Directory(File directory) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        Stream.of(S3Action.values()).forEach(action -> action.setDirectory(directory));
        this.s3Directory = directory;
    }

    public void setEndpointHost(String endpointHost) {
        this.endpointHost = endpointHost;
    }

}
