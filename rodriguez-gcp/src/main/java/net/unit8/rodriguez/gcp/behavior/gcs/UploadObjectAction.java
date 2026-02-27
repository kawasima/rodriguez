package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
 */
public class UploadObjectAction extends GCSActionBase<Map<String, Object>> {
    /** Creates a new UploadObjectAction. */
    public UploadObjectAction() {
    }

    @Override
    public Map<String, Object> handle(GCSRequest request) {
        String bucketName = request.getQueryParam("_bucketName");
        String objectName = request.getQueryParam("name");
        if (objectName == null) {
            objectName = request.getQueryParam("_objectName");
        }

        String uploadType = request.getQueryParam("uploadType");
        byte[] data;
        try {
            byte[] rawBody = request.getBody().readAllBytes();
            if ("multipart".equals(uploadType)) {
                data = extractMultipartData(rawBody, request.getHeader("content-type"));
            } else {
                data = rawBody;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String finalObjectName = objectName;
        Optional.ofNullable(getGcsDirectory())
                .map(dir -> Path.of(dir.getPath(), bucketName, finalObjectName))
                .ifPresent(path -> {
                    try {
                        Files.createDirectories(path.getParent());
                        Files.write(path, data);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        String md5Hash = computeMd5Base64(data);
        String crc32c = computeCrc32cBase64(data);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kind", "storage#object");
        response.put("id", bucketName + "/" + objectName + "/1");
        response.put("name", objectName);
        response.put("bucket", bucketName);
        response.put("size", String.valueOf(data.length));
        response.put("contentType", "application/octet-stream");
        response.put("md5Hash", md5Hash);
        response.put("crc32c", crc32c);
        response.put("timeCreated", Instant.now().toString());
        response.put("updated", Instant.now().toString());
        response.put("storageClass", "STANDARD");
        response.put("etag", "CLk=");
        return response;
    }

    private byte[] extractMultipartData(byte[] rawBody, String contentType) {
        // Extract boundary from content-type: multipart/related; boundary=xxx
        String boundary = null;
        if (contentType != null) {
            for (String part : contentType.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("boundary=")) {
                    boundary = trimmed.substring("boundary=".length()).trim();
                    // Remove quotes if present
                    if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                        boundary = boundary.substring(1, boundary.length() - 1);
                    }
                    break;
                }
            }
        }
        if (boundary == null) {
            return rawBody;
        }

        // Parse multipart: skip first part (JSON metadata), return second part (binary data)
        String bodyStr = new String(rawBody, StandardCharsets.UTF_8);
        String delimiter = "--" + boundary;
        String[] parts = bodyStr.split(delimiter);
        // parts[0] = empty (before first boundary)
        // parts[1] = first part (metadata JSON)
        // parts[2] = second part (actual data)
        // parts[3] = "--" (closing)
        if (parts.length >= 3) {
            String dataPart = parts[2];
            // Skip headers (separated by double newline)
            int headerEnd = dataPart.indexOf("\r\n\r\n");
            if (headerEnd == -1) {
                headerEnd = dataPart.indexOf("\n\n");
                if (headerEnd >= 0) {
                    return dataPart.substring(headerEnd + 2).trim().getBytes(StandardCharsets.UTF_8);
                }
            } else {
                return dataPart.substring(headerEnd + 4).trim().getBytes(StandardCharsets.UTF_8);
            }
        }
        return rawBody;
    }

    private String computeMd5Base64(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private String computeCrc32cBase64(byte[] data) {
        CRC32C crc = new CRC32C();
        crc.update(data);
        long value = crc.getValue();
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((value >> 24) & 0xFF);
        bytes[1] = (byte) ((value >> 16) & 0xFF);
        bytes[2] = (byte) ((value >> 8) & 0xFF);
        bytes[3] = (byte) (value & 0xFF);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
