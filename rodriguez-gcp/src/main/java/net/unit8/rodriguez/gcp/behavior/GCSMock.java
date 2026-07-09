package net.unit8.rodriguez.gcp.behavior;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityBehavior;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.gcp.GCSException;
import net.unit8.rodriguez.gcp.GCSRequest;
import net.unit8.rodriguez.gcp.behavior.gcs.GCSAction;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
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

    /** Maximum accepted size for a metadata (non-upload) request body (64 MB). */
    static final long MAX_METADATA_BODY_SIZE = 64L * 1024 * 1024;

    private final ObjectMapper mapper;
    private final long maxMetadataBodySize;
    private File gcsDirectory;

    /**
     * Creates a new GCSMock instance with a Jackson {@link ObjectMapper}
     * configured for Java time serialization.
     */
    public GCSMock() {
        this(MAX_METADATA_BODY_SIZE);
    }

    /**
     * Creates a new GCSMock instance with an explicit metadata body size cap.
     *
     * @param maxMetadataBodySize the maximum accepted size for a non-upload request body
     */
    GCSMock(long maxMetadataBodySize) {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        this.maxMetadataBodySize = maxMetadataBodySize;
    }

    private synchronized void ensureGcsDirectory() {
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
                byte[] body = readBounded(request.getBody(), maxMetadataBodySize);
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
        } catch (GCSException e) {
            LOG.warning("GCSMock: " + e.getStatusCode() + " " + e.getMessage());
            getMetricRegistry().counter(MetricRegistry.name(GCSMock.class, "client-error")).inc();
            try {
                sendError(exchange, e.getStatusCode(), e.getMessage());
            } catch (IOException ignore) {
            }
        } catch (Exception e) {
            LOG.severe("GCSMock error: " + e.getMessage());
            getMetricRegistry().counter(MetricRegistry.name(GCSMock.class, "other-error")).inc();
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (IOException ignore) {
            }
        } finally {
            exchange.close();
        }
    }

    /**
     * Writes a GCS JSON API style error response.
     *
     * @param exchange the HTTP exchange to write to
     * @param status   the HTTP status code
     * @param message  the error message
     * @throws IOException if writing the response fails
     */
    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> error = Map.of(
                "error", Map.of(
                        "code", status,
                        "message", message,
                        "errors", List.of(Map.of(
                                "domain", "global",
                                "reason", "invalid",
                                "message", message))));
        byte[] body = mapper.writeValueAsBytes(error);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    /**
     * Reads the request body into memory, bounded by {@code limit}, so that a
     * chunked body with no {@code Content-Length} cannot exhaust the heap. Reads
     * at most {@code limit + 1} bytes and rejects anything larger with a 413.
     *
     * @param in    the request body stream
     * @param limit the maximum accepted body size in bytes
     * @return the body bytes (never larger than {@code limit})
     * @throws GCSException if the body exceeds {@code limit}
     */
    private static byte[] readBounded(InputStream in, long limit) throws IOException {
        // Bound the in-memory buffer to a Java array's capacity and detect
        // overflow of `limit + 1` explicitly.
        long effectiveLimit = Math.min(limit, (long) Integer.MAX_VALUE - 8);
        int cap = (int) (effectiveLimit + 1);
        byte[] data = in.readNBytes(cap);
        if (data.length > effectiveLimit) {
            throw new GCSException(413, "Request body exceeds maximum allowed size of " + effectiveLimit + " bytes");
        }
        return data;
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
