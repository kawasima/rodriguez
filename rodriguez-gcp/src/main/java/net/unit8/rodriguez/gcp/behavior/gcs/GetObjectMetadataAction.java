package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSRequest;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles {@code GET /storage/v1/b/{bucket}/o/{object}} to retrieve GCS object metadata.
 *
 * <p>Returns a JSON response containing object metadata such as name, size,
 * content type, and timestamps.
 */
public class GetObjectMetadataAction extends GCSActionBase<Map<String, Object>> {
    /** Creates a new GetObjectMetadataAction. */
    public GetObjectMetadataAction() {
    }

    @Override
    public Map<String, Object> handle(GCSRequest request) {
        String bucketName = request.getQueryParam("_bucketName");
        String objectName = request.getQueryParam("_objectName");

        File file = getGcsDirectory().toPath().resolve(bucketName).resolve(objectName).toFile();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kind", "storage#object");
        response.put("id", bucketName + "/" + objectName + "/1");
        response.put("name", objectName);
        response.put("bucket", bucketName);
        response.put("size", String.valueOf(file.length()));
        response.put("contentType", "application/octet-stream");
        response.put("timeCreated", Instant.ofEpochMilli(file.lastModified()).toString());
        response.put("updated", Instant.ofEpochMilli(file.lastModified()).toString());
        response.put("storageClass", "STANDARD");
        response.put("etag", "CLk=");
        return response;
    }
}
