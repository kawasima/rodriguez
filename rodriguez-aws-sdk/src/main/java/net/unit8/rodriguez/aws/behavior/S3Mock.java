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
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Mock implementation of Amazon S3 as an HTTP instability behavior.
 *
 * <p>Supports basic S3 operations (create/delete bucket, put/get/delete object,
 * list buckets/objects) backed by a local filesystem directory.</p>
 */
public class S3Mock implements HttpInstabilityBehavior, MetricsAvailable {
    private static final Logger LOG = Logger.getLogger(S3Mock.class.getName());

    private static final DateTimeFormatter HTTP_DATE = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
    private final XmlMapper mapper;
    private File s3Directory;
    private String endpointHost = "localhost";

    /**
     * Constructs an S3Mock instance with a pre-configured XML mapper for S3 responses.
     */
    public S3Mock() {
        mapper = XmlMapper.builder()
                .addModule(new JavaTimeModule()
                        .addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))))
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

    private void ensureS3Directory() {
        if (s3Directory == null) {
            try {
                s3Directory = Files.createTempDirectory("rodriguez-s3").toFile();
                s3Directory.deleteOnExit();
                Stream.of(S3Action.values()).forEach(action -> action.setDirectory(s3Directory));
                LOG.info("S3Mock: using temporary directory " + s3Directory);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create temporary S3 directory", e);
            }
        }
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            ensureS3Directory();
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
            if (response instanceof ErrorResponse errorResponse) {
                errorResponse.handle(exchange);
            } else if (response == null) {
                exchange.sendResponseHeaders(200, -1);
            } else if (response instanceof File f) {
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
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
                exchange.getResponseHeaders().set("Content-Type", "application/xml");
                exchange.sendResponseHeaders(200, 0);
                LOG.fine(mapper.writeValueAsString(response));
                mapper.writeValue(exchange.getResponseBody(), response);
            }
        } catch (Exception e) {
            LOG.severe("S3Mock error: " + e.getMessage());
            getMetricRegistry().counter(MetricRegistry.name(S3Mock.class, "other-error"));
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch(IOException ignore) {

            }
        } finally {
            exchange.close();
        }
    }

    /**
     * Sets the filesystem directory used to store S3 bucket and object data.
     *
     * @param directory the directory to use for S3 storage; must be an existing directory
     * @throws IllegalArgumentException if the given path is not a directory
     */
    public void setS3Directory(File directory) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(directory + " is not a directory");
        }
        Stream.of(S3Action.values()).forEach(action -> action.setDirectory(directory));
        this.s3Directory = directory;
    }

    /**
     * Sets the endpoint hostname used for virtual-hosted-style bucket name resolution.
     *
     * @param endpointHost the endpoint hostname (e.g., {@code "localhost"})
     */
    public void setEndpointHost(String endpointHost) {
        this.endpointHost = endpointHost;
    }

}
