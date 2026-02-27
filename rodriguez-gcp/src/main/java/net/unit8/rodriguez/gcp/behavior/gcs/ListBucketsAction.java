package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles {@code GET /storage/v1/b} to list all GCS buckets.
 *
 * <p>Returns a {@code storage#buckets} JSON response containing all bucket
 * subdirectories under the GCS root directory.
 */
public class ListBucketsAction extends GCSActionBase<Map<String, Object>> {
    /** Creates a new ListBucketsAction. */
    public ListBucketsAction() {
    }

    @Override
    public Map<String, Object> handle(GCSRequest request) {
        List<Map<String, Object>> items = getBucketDirectories().stream()
                .map(f -> {
                    Map<String, Object> bucket = new LinkedHashMap<>();
                    bucket.put("kind", "storage#bucket");
                    bucket.put("id", f.getName());
                    bucket.put("name", f.getName());
                    bucket.put("timeCreated", Instant.ofEpochMilli(f.lastModified()).toString());
                    bucket.put("updated", Instant.ofEpochMilli(f.lastModified()).toString());
                    bucket.put("location", "US");
                    bucket.put("storageClass", "STANDARD");
                    return bucket;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kind", "storage#buckets");
        response.put("items", items);
        return response;
    }
}
