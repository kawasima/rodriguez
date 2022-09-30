package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.ErrorResponse;
import net.unit8.rodriguez.aws.MockAction;

import java.io.File;
import java.util.function.Function;

import static com.amazonaws.HttpMethod.*;

public enum S3Action {
    PutObject(new PutObjectAction(), req -> req.getMethod() == PUT && req.getParams().getFirst("ObjectName") != null),
    CreateBucket(new CreateBucketAction(), req -> req.getMethod() == PUT),
    ListBuckets(new ListBucketsAction(), req -> req.getMethod() == GET && !req.getParams().containsKey("BucketName")),
    ListObjects(new ListObjectsAction(), req -> req.getMethod() == GET && !req.getParams().containsKey("ObjectName")),
    GetObject(new GetObjectAction(), req -> req.getMethod() == GET),
    DeleteObject(new DeleteObjectAction(), req -> req.getMethod() == DELETE && req.getParams().containsKey("ObjectName")),
    DeleteBucket(new DeleteBucketAction(), req -> req.getMethod() == DELETE),
    NotFound(params -> ErrorResponse.notFound(), req -> false)
    ;

    private final MockAction<?> mockAction;
    private final Function<AWSRequest, Boolean> applicableFunc;

    S3Action(MockAction<?> mockAction, Function<AWSRequest, Boolean> applicableFunc) {
        this.mockAction = mockAction;
        this.applicableFunc = applicableFunc;
    }

    public boolean isApplicable(AWSRequest request) {
        return applicableFunc.apply(request);
    }
    public <T> T handle(AWSRequest request) {
        return (T) mockAction.handle(request);
    }

    public void setDirectory(File directory) {
        if (mockAction instanceof S3ActionBase) {
            ((S3ActionBase<?>) mockAction).setS3Directory(directory);
        }
    }
}
