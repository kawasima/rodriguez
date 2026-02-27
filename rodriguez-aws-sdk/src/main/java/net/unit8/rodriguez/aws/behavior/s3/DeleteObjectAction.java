package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.AWSRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles the S3 DeleteObject API operation by deleting the object file from the bucket directory.
 */
public class DeleteObjectAction extends S3ActionBase<Void> {

    /**
     * Constructs a DeleteObjectAction.
     */
    public DeleteObjectAction() {
    }

    @Override
    public Void handle(AWSRequest request) {
        String bucketName = request.getParams().getFirst("BucketName");
        String objectName = request.getParams().getFirst("ObjectName");

        Path objectFile = getS3Directory().toPath().resolve(bucketName).resolve(objectName);
        try {
            Files.delete(objectFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return null;
    }
}
