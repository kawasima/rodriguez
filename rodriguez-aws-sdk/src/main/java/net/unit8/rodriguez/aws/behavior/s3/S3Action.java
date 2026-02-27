package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.ErrorResponse;
import net.unit8.rodriguez.aws.MockAction;

import java.io.File;
import java.util.function.Function;

import static com.amazonaws.HttpMethod.*;

/**
 * Enumeration of S3 API actions with their applicability predicates and action handlers.
 *
 * <p>Each constant maps an incoming request to the appropriate {@link MockAction}
 * based on HTTP method and request parameters.</p>
 */
public enum S3Action {
    /** Handles the S3 PutObject API operation. */
    PutObject(new PutObjectAction(), req -> req.getMethod() == PUT && req.getParams().getFirst("ObjectName") != null),
    /** Handles the S3 CreateBucket API operation. */
    CreateBucket(new CreateBucketAction(), req -> req.getMethod() == PUT),
    /** Handles the S3 ListBuckets API operation. */
    ListBuckets(new ListBucketsAction(), req -> req.getMethod() == GET && !req.getParams().containsKey("BucketName")),
    /** Handles the S3 ListObjects API operation. */
    ListObjects(new ListObjectsAction(), req -> req.getMethod() == GET && !req.getParams().containsKey("ObjectName")),
    /** Handles the S3 GetObject API operation. */
    GetObject(new GetObjectAction(), req -> req.getMethod() == GET),
    /** Handles the S3 DeleteObject API operation. */
    DeleteObject(new DeleteObjectAction(), req -> req.getMethod() == DELETE && req.getParams().containsKey("ObjectName")),
    /** Handles the S3 DeleteBucket API operation. */
    DeleteBucket(new DeleteBucketAction(), req -> req.getMethod() == DELETE),
    /** Returns a 404 Not Found error response for unmatched requests. */
    NotFound(params -> ErrorResponse.notFound(), req -> false)
    ;

    private final MockAction<?> mockAction;
    private final Function<AWSRequest, Boolean> applicableFunc;

    S3Action(MockAction<?> mockAction, Function<AWSRequest, Boolean> applicableFunc) {
        this.mockAction = mockAction;
        this.applicableFunc = applicableFunc;
    }

    /**
     * Tests whether this action is applicable to the given request.
     *
     * @param request the incoming AWS request
     * @return {@code true} if this action can handle the request
     */
    public boolean isApplicable(AWSRequest request) {
        return applicableFunc.apply(request);
    }

    /**
     * Delegates the request to the underlying mock action handler.
     *
     * @param request the incoming AWS request
     * @param <T> the response type
     * @return the response produced by the action handler
     */
    public <T> T handle(AWSRequest request) {
        return (T) mockAction.handle(request);
    }

    /**
     * Sets the filesystem directory on the underlying action if it extends {@link S3ActionBase}.
     *
     * @param directory the S3 storage directory
     */
    public void setDirectory(File directory) {
        if (mockAction instanceof S3ActionBase<?> base) {
            base.setS3Directory(directory);
        }
    }
}
