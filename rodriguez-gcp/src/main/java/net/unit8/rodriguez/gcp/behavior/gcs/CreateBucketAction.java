package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSRequest;

import java.io.File;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles {@code POST /storage/v1/b} to create a new GCS bucket.
 *
 * <p>Creates a subdirectory under the GCS root to represent the bucket.
 * If the bucket already exists, this operation is idempotent.
 */
public class CreateBucketAction extends GCSActionBase<Map<String, Object>> {
    /** Creates a new CreateBucketAction. */
    public CreateBucketAction() {
    }

    @Override
    public Map<String, Object> handle(GCSRequest request) {
        String bucketName = request.getQueryParam("_bucketName");
        File bucketDir = new File(getGcsDirectory(), bucketName);
        if (!bucketDir.exists()) {
            bucketDir.mkdir();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kind", "storage#bucket");
        response.put("id", bucketName);
        response.put("name", bucketName);
        response.put("timeCreated", Instant.now().toString());
        response.put("updated", Instant.now().toString());
        response.put("location", "US");
        response.put("storageClass", "STANDARD");
        response.put("etag", "CAE=");
        return response;
    }
}
