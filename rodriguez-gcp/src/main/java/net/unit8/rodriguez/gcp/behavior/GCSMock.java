package net.unit8.rodriguez.gcp.behavior;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityBehavior;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.gcp.GCSRequest;
import net.unit8.rodriguez.gcp.behavior.gcs.GCSAction;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Mock implementation of the Google Cloud Storage JSON API v1.
 *
 * <p>Supported endpoints:
 * <ul>
 *   <li>POST   /storage/v1/b?project=...                        — Create bucket</li>
 *   <li>GET    /storage/v1/b?project=...                        — List buckets</li>
 *   <li>DELETE  /storage/v1/b/{bucket}                           — Delete bucket</li>
 *   <li>POST   /upload/storage/v1/b/{bucket}/o?uploadType=media  — Upload object</li>
 *   <li>GET    /storage/v1/b/{bucket}/o                          — List objects</li>
 *   <li>GET    /storage/v1/b/{bucket}/o/{object}                 — Get object metadata</li>
 *   <li>GET    /storage/v1/b/{bucket}/o/{object}?alt=media       — Download object</li>
 *   <li>DELETE  /storage/v1/b/{bucket}/o/{object}                — Delete object</li>
 * </ul>
 */
public class GCSMock implements HttpInstabilityBehavior, MetricsAvailable {
    private static final Logger LOG = Logger.getLogger(GCSMock.class.getName());

    private static final Pattern UPLOAD_OBJECT_PATH = Pattern.compile("^/upload/storage/v1/b/([^/]+)/o$");
    private static final Pattern BUCKET_OBJECT_PATH = Pattern.compile("^/storage/v1/b/([^/]+)/o/(.+)$");
    private static final Pattern BUCKET_OBJECTS_PATH = Pattern.compile("^/storage/v1/b/([^/]+)/o$");
    private static final Pattern BUCKET_PATH = Pattern.compile("^/storage/v1/b/([^/]+)$");
    private static final Pattern BUCKETS_PATH = Pattern.compile("^/storage/v1/b/?$");

    private final ObjectMapper mapper;
    private File gcsDirectory;

    /**
     * Creates a new GCSMock instance with a Jackson {@link ObjectMapper}
     * configured for Java time serialization.
     */
    public GCSMock() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    private void ensureGcsDirectory() {
        if (gcsDirectory == null) {
            try {
                gcsDirectory = Files.createTempDirectory("rodriguez-gcs").toFile();
                gcsDirectory.deleteOnExit();
                Stream.of(GCSAction.values()).forEach(action -> action.setDirectory(gcsDirectory));
                LOG.info("GCSMock: using temporary directory " + gcsDirectory);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to create temporary GCS directory", e);
            }
        }
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            ensureGcsDirectory();
            GCSRequest request = GCSRequest.of(exchange);
            String path = exchange.getRequestURI().getPath();

            // Parse path to extract bucket name and object name, store as internal params
            parsePath(request, path);

            // Handle bucket name from JSON body for CreateBucket (POST /storage/v1/b)
            if ("POST".equals(request.getMethod()) && !path.startsWith("/upload/")) {
                byte[] body = request.getBody().readAllBytes();
                if (body.length > 0) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> json = mapper.readValue(body, Map.class);
                    String bucketName = (String) json.get("name");
                    if (bucketName != null) {
                        request.getQueryParams().put("_bucketName", bucketName);
                    }
                }
            }

            GCSAction action = Stream.of(GCSAction.values())
                    .filter(act -> act.isApplicable(request))
                    .findFirst()
                    .orElse(null);

            if (action == null) {
                LOG.warning("GCSMock: no action found for " + request.getMethod() + " " + path);
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            Object response = action.handle(request);
            if (response == null) {
                // 204 No Content for delete operations
                exchange.sendResponseHeaders(204, -1);
            } else if (response instanceof File f) {
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                exchange.sendResponseHeaders(200, f.length());
                byte[] buffer = new byte[4096];
                try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(f))) {
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        exchange.getResponseBody().write(buffer, 0, read);
                    }
                }
            } else {
                byte[] jsonBytes = mapper.writeValueAsBytes(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, jsonBytes.length);
                exchange.getResponseBody().write(jsonBytes);
            }
        } catch (Exception e) {
            LOG.severe("GCSMock error: " + e.getMessage());
            getMetricRegistry().counter(MetricRegistry.name(GCSMock.class, "other-error"));
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (IOException ignore) {
            }
        } finally {
            exchange.close();
        }
    }

    private void parsePath(GCSRequest request, String path) {
        Matcher m;

        // /upload/storage/v1/b/{bucket}/o
        m = UPLOAD_OBJECT_PATH.matcher(path);
        if (m.matches()) {
            request.getQueryParams().put("_bucketName", m.group(1));
            return;
        }

        // /storage/v1/b/{bucket}/o/{object}
        m = BUCKET_OBJECT_PATH.matcher(path);
        if (m.matches()) {
            request.getQueryParams().put("_bucketName", m.group(1));
            request.getQueryParams().put("_objectName", m.group(2));
            return;
        }

        // /storage/v1/b/{bucket}/o
        m = BUCKET_OBJECTS_PATH.matcher(path);
        if (m.matches()) {
            request.getQueryParams().put("_bucketName", m.group(1));
            return;
        }

        // /storage/v1/b/{bucket}
        m = BUCKET_PATH.matcher(path);
        if (m.matches()) {
            request.getQueryParams().put("_bucketName", m.group(1));
            return;
        }

        // /storage/v1/b — list buckets (no bucket name in path)
    }
}
