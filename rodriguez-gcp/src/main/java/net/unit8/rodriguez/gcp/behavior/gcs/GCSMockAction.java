package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSRequest;

/**
 * Functional interface for GCS mock action handlers.
 *
 * <p>Each implementation handles a specific GCS API operation
 * (e.g., create bucket, upload object) and returns a typed response.
 *
 * @param <T> the response type produced by this action
 */
public interface GCSMockAction<T> {
    /**
     * Handles the given GCS request and returns a response.
     *
     * @param request the incoming GCS request
     * @return the response object
     */
    T handle(GCSRequest request);
}
