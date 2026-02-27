package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSRequest;

import java.io.File;
import java.util.function.Function;

/**
 * Enum-based action router for the GCS mock server.
 *
 * <p>Each enum constant maps to a specific GCS API operation and includes
 * a predicate to determine whether a given request matches. The first
 * matching action (in declaration order) handles the request.
 */
public enum GCSAction {
    /** Upload an object ({@code POST /upload/storage/v1/b/{bucket}/o}). */
    UploadObject(new UploadObjectAction(),
            req -> "POST".equals(req.getMethod()) && req.getRequestURI().getPath().startsWith("/upload/")),
    /** Create a bucket ({@code POST /storage/v1/b}). */
    CreateBucket(new CreateBucketAction(),
            req -> "POST".equals(req.getMethod())),
    /** Download an object ({@code GET /storage/v1/b/{bucket}/o/{object}?alt=media}). */
    GetObject(new GetObjectAction(),
            req -> "GET".equals(req.getMethod()) && req.getQueryParam("_objectName") != null && "media".equals(req.getQueryParam("alt"))),
    /** Get object metadata ({@code GET /storage/v1/b/{bucket}/o/{object}}). */
    GetObjectMetadata(new GetObjectMetadataAction(),
            req -> "GET".equals(req.getMethod()) && req.getQueryParam("_objectName") != null),
    /** List objects in a bucket ({@code GET /storage/v1/b/{bucket}/o}). */
    ListObjects(new ListObjectsAction(),
            req -> "GET".equals(req.getMethod()) && req.getQueryParam("_bucketName") != null && req.getQueryParam("_objectName") == null),
    /** List all buckets ({@code GET /storage/v1/b}). */
    ListBuckets(new ListBucketsAction(),
            req -> "GET".equals(req.getMethod()) && req.getQueryParam("_bucketName") == null),
    /** Delete an object ({@code DELETE /storage/v1/b/{bucket}/o/{object}}). */
    DeleteObject(new DeleteObjectAction(),
            req -> "DELETE".equals(req.getMethod()) && req.getQueryParam("_objectName") != null),
    /** Delete a bucket ({@code DELETE /storage/v1/b/{bucket}}). */
    DeleteBucket(new DeleteBucketAction(),
            req -> "DELETE".equals(req.getMethod())),
    ;

    private final GCSMockAction<?> mockAction;
    private final Function<GCSRequest, Boolean> applicableFunc;

    GCSAction(GCSMockAction<?> mockAction, Function<GCSRequest, Boolean> applicableFunc) {
        this.mockAction = mockAction;
        this.applicableFunc = applicableFunc;
    }

    /**
     * Tests whether this action can handle the given request.
     *
     * @param request the incoming GCS request
     * @return {@code true} if this action matches the request
     */
    public boolean isApplicable(GCSRequest request) {
        return applicableFunc.apply(request);
    }

    /**
     * Delegates request handling to the underlying action implementation.
     *
     * @param request the incoming GCS request
     * @param <T> the response type
     * @return the action response
     */
    @SuppressWarnings("unchecked")
    public <T> T handle(GCSRequest request) {
        return (T) mockAction.handle(request);
    }

    /**
     * Sets the filesystem directory for actions that use file-backed storage.
     *
     * @param directory the root directory for GCS storage
     */
    public void setDirectory(File directory) {
        if (mockAction instanceof GCSActionBase<?> base) {
            base.setGcsDirectory(directory);
        }
    }
}
