package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSException;
import net.unit8.rodriguez.gcp.GCSRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.zip.CRC32C;

/**
 * Handles {@code POST /upload/storage/v1/b/{bucket}/o} to upload a GCS object.
 *
 * <p>Supports both {@code uploadType=media} (raw body) and {@code uploadType=multipart}
 * ({@code multipart/related} with JSON metadata and binary data).
 * Computes MD5 and CRC32C checksums for integrity validation by the client.
 *
 * <p>Uploads are capped at a configurable maximum size (default 64 MB) to avoid
 * unbounded buffering. The cap can be overridden with the
 * {@code rodriguez.gcs.maxUploadBytes} system property. Media uploads are streamed
 * directly to disk; multipart uploads are buffered (bounded) so the parts can be split.
 */
public class UploadObjectAction extends GCSActionBase<Map<String, Object>> {
    /** Default maximum upload body size in bytes (64 MB). */
    static final long DEFAULT_MAX_BODY_SIZE = 64L * 1024 * 1024;

    private static final byte[] CRLF_CRLF = {'\r', '\n', '\r', '\n'};
    private static final byte[] LF_LF = {'\n', '\n'};

    private final long maxBodySize;

    /** Creates a new UploadObjectAction with the configured maximum body size. */
    public UploadObjectAction() {
        this(resolveMaxBodySize());
    }

    /**
     * Creates a new UploadObjectAction with an explicit maximum body size.
     *
     * @param maxBodySize the maximum accepted upload size in bytes
     */
    UploadObjectAction(long maxBodySize) {
        this.maxBodySize = maxBodySize;
    }

    private static long resolveMaxBodySize() {
        String prop = System.getProperty("rodriguez.gcs.maxUploadBytes");
        if (prop != null) {
            try {
                long value = Long.parseLong(prop.trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ignore) {
                // fall through to default
            }
        }
        return DEFAULT_MAX_BODY_SIZE;
    }

    @Override
    public Map<String, Object> handle(GCSRequest request) {
        String bucketName = request.getQueryParam("_bucketName");
        String objectName = request.getQueryParam("name");
        if (objectName == null) {
            objectName = request.getQueryParam("_objectName");
        }

        // Resolve (and validate) the target path before reading the body so that
        // path-traversal attempts are rejected without consuming a large upload.
        Path target = resolveObjectPath(bucketName, objectName);
        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String uploadType = request.getQueryParam("uploadType");
        UploadResult result;
        if ("multipart".equals(uploadType)) {
            byte[] rawBody = readBounded(request.getBody(), maxBodySize);
            byte[] data = extractMultipartData(rawBody, request.getHeader("content-type"));
            try {
                Files.write(target, data);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            result = new UploadResult(data.length, computeMd5Base64(data), computeCrc32cBase64(data));
        } else {
            result = streamToFile(request.getBody(), target, maxBodySize);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kind", "storage#object");
        response.put("id", bucketName + "/" + objectName + "/1");
        response.put("name", objectName);
        response.put("bucket", bucketName);
        response.put("size", String.valueOf(result.size()));
        response.put("contentType", "application/octet-stream");
        response.put("md5Hash", result.md5Hash());
        response.put("crc32c", result.crc32c());
        response.put("timeCreated", Instant.now().toString());
        response.put("updated", Instant.now().toString());
        response.put("storageClass", "STANDARD");
        response.put("etag", "CLk=");
        return response;
    }

    private record UploadResult(long size, String md5Hash, String crc32c) {
    }

    /**
     * Streams the request body directly to {@code target}, enforcing the size cap
     * and computing MD5/CRC32C checksums on the fly so the whole object is never
     * buffered in memory.
     */
    private UploadResult streamToFile(InputStream in, Path target, long limit) {
        MessageDigest md5 = newMd5();
        CRC32C crc = new CRC32C();
        long total = 0;
        byte[] buffer = new byte[8192];
        try (OutputStream out = Files.newOutputStream(target)) {
            int read;
            while ((read = in.read(buffer)) > 0) {
                total += read;
                if (total > limit) {
                    throw new GCSException(413, "Upload exceeds maximum allowed size of " + limit + " bytes");
                }
                if (md5 != null) {
                    md5.update(buffer, 0, read);
                }
                crc.update(buffer, 0, read);
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            deleteQuietly(target);
            throw new UncheckedIOException(e);
        } catch (GCSException e) {
            deleteQuietly(target);
            throw e;
        }
        return new UploadResult(total, encodeMd5(md5), encodeCrc32c(crc));
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignore) {
            // best-effort cleanup
        }
    }

    /**
     * Reads the whole body into memory, bounded by {@code limit}. Used for the
     * multipart case, where the parts must be split before the payload is known.
     */
    private static byte[] readBounded(InputStream in, long limit) {
        try {
            // Multipart bodies are buffered fully in memory, so the effective cap
            // can never exceed a Java array's capacity. Clamp `limit` to that bound
            // before computing `limit + 1` so the read cap cannot overflow to a
            // negative int (which would throw) nor silently saturate at
            // Integer.MAX_VALUE while the size check below never fires.
            long effectiveLimit = Math.min(limit, (long) Integer.MAX_VALUE - 8);
            int cap = (int) (effectiveLimit + 1);
            byte[] data = in.readNBytes(cap);
            if (data.length > effectiveLimit) {
                throw new GCSException(413, "Upload exceeds maximum allowed size of " + effectiveLimit + " bytes");
            }
            return data;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] extractMultipartData(byte[] rawBody, String contentType) {
        String boundary = parseBoundary(contentType);
        if (boundary == null) {
            return rawBody;
        }

        // Split on the boundary as literal bytes (never as a regex) so that a
        // hostile boundary cannot trigger a PatternSyntaxException or a
        // catastrophic-backtracking DoS. The payload is extracted byte-exact so
        // that non-UTF-8 / whitespace-edge binary uploads survive intact.
        byte[] delimiter = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);
        int first = indexOf(rawBody, delimiter, 0);
        if (first < 0) {
            return rawBody;
        }
        int second = indexOf(rawBody, delimiter, first + delimiter.length);
        if (second < 0) {
            return rawBody;
        }
        int third = indexOf(rawBody, delimiter, second + delimiter.length);
        if (third < 0) {
            third = rawBody.length;
        }

        // The data part lives between the second and third boundary. Skip its
        // MIME headers (terminated by a blank line).
        int partStart = second + delimiter.length;
        int sepLen = CRLF_CRLF.length;
        int headerEnd = indexOf(rawBody, CRLF_CRLF, partStart);
        if (headerEnd < 0 || headerEnd >= third) {
            headerEnd = indexOf(rawBody, LF_LF, partStart);
            sepLen = LF_LF.length;
        }
        if (headerEnd < 0 || headerEnd >= third) {
            return rawBody;
        }

        int dataStart = headerEnd + sepLen;
        int dataEnd = third;
        // Strip exactly the line terminator that precedes the closing boundary,
        // without touching any trailing whitespace that belongs to the payload.
        if (dataEnd - 2 >= dataStart && rawBody[dataEnd - 2] == '\r' && rawBody[dataEnd - 1] == '\n') {
            dataEnd -= 2;
        } else if (dataEnd - 1 >= dataStart && rawBody[dataEnd - 1] == '\n') {
            dataEnd -= 1;
        }
        if (dataEnd < dataStart) {
            dataEnd = dataStart;
        }
        return Arrays.copyOfRange(rawBody, dataStart, dataEnd);
    }

    private static String parseBoundary(String contentType) {
        if (contentType == null) {
            return null;
        }
        for (String part : contentType.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("boundary=")) {
                String boundary = trimmed.substring("boundary=".length()).trim();
                if (boundary.length() >= 2 && boundary.startsWith("\"") && boundary.endsWith("\"")) {
                    boundary = boundary.substring(1, boundary.length() - 1);
                }
                return boundary.isEmpty() ? null : boundary;
            }
        }
        return null;
    }

    private static int indexOf(byte[] array, byte[] target, int from) {
        if (target.length == 0) {
            return Math.max(from, 0);
        }
        outer:
        for (int i = Math.max(from, 0); i <= array.length - target.length; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static MessageDigest newMd5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String encodeMd5(MessageDigest md5) {
        if (md5 == null) {
            return "";
        }
        return Base64.getEncoder().encodeToString(md5.digest());
    }

    private static String encodeCrc32c(CRC32C crc) {
        long value = crc.getValue();
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((value >> 24) & 0xFF);
        bytes[1] = (byte) ((value >> 16) & 0xFF);
        bytes[2] = (byte) ((value >> 8) & 0xFF);
        bytes[3] = (byte) (value & 0xFF);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String computeMd5Base64(byte[] data) {
        MessageDigest md5 = newMd5();
        if (md5 == null) {
            return "";
        }
        return Base64.getEncoder().encodeToString(md5.digest(data));
    }

    private String computeCrc32cBase64(byte[] data) {
        CRC32C crc = new CRC32C();
        crc.update(data);
        return encodeCrc32c(crc);
    }
}
