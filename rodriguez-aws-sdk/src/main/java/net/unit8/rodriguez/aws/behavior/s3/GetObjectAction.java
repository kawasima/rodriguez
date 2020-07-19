package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.AWSRequest;

import java.io.File;

public class GetObjectAction extends S3ActionBase<File> {
    @Override
    public File handle(AWSRequest request) {
        String bucketName = request.getParams().getFirst("BucketName");
        String objectName = request.getParams().getFirst("ObjectName");
        return getS3Directory().toPath().resolve(bucketName).resolve(objectName).toFile();
    }
}
