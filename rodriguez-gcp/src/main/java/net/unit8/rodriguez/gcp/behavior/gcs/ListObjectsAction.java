package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSRequest;

import java.io.File;
import java.time.Instant;
import java.util.*;

/**
 * Handles {@code GET /storage/v1/b/{bucket}/o} to list objects in a GCS bucket.
 *
 * <p>Returns a {@code storage#objects} JSON response containing all files
 * within the specified bucket directory.
 */
public class ListObjectsAction extends GCSActionBase<Map<String, Object>> {
    /** Creates a new ListObjectsAction. */
    public ListObjectsAction() {
    }

    @Override
    public Map<String, Object> handle(GCSRequest request) {
        String bucketName = request.getQueryParam("_bucketName");

        List<Map<String, Object>> items = Optional.ofNullable(getGcsDirectory())
                .map(dir -> new File(dir, bucketName))
                .filter(File::isDirectory)
                .map(File::listFiles)
                .map(files -> Arrays.stream(files)
                        .filter(File::isFile)
                        .map(f -> {
                            Map<String, Object> obj = new LinkedHashMap<>();
                            obj.put("kind", "storage#object");
                            obj.put("id", bucketName + "/" + f.getName() + "/1");
                            obj.put("name", f.getName());
                            obj.put("bucket", bucketName);
                            obj.put("size", String.valueOf(f.length()));
                            obj.put("timeCreated", Instant.ofEpochMilli(f.lastModified()).toString());
                            obj.put("updated", Instant.ofEpochMilli(f.lastModified()).toString());
                            obj.put("storageClass", "STANDARD");
                            return obj;
                        })
                        .toList())
                .orElse(Collections.emptyList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kind", "storage#objects");
        response.put("items", items);
        return response;
    }
}
