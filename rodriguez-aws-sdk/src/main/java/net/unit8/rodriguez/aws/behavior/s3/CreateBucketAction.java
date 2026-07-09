package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.AWSRequest;

/**
 * Handles the S3 CreateBucket API operation by creating a subdirectory in the S3 storage directory.
 */
public class CreateBucketAction extends S3ActionBase<Void> {

    /**
     * Constructs a CreateBucketAction.
     */
    public CreateBucketAction() {
    }

    @Override
    public Void handle(AWSRequest request) {
        String bucketName = request.getParams().getFirst("BucketName");
        if (!resolveBucketPath(bucketName).toFile().mkdir()) {
            throw new RuntimeException("Fail to create a bucket");
        }
        return null;
    }
}
