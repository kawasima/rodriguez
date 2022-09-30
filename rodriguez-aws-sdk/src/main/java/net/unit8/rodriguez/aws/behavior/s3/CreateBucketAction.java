package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.AWSRequest;

import java.io.File;
import java.util.Optional;

public class CreateBucketAction extends S3ActionBase<Void> {
    @Override
    public Void handle(AWSRequest request) {
        String bucketName = request.getParams().getFirst("BucketName");
        Optional.ofNullable(getS3Directory())
                .filter(dir -> new File(dir, bucketName).mkdir())
                .orElseThrow(() -> new RuntimeException("Fail to create a bucket"));
        return null;
    }
}
