package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.AWSRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Handles the S3 PutObject API operation by writing the request body to a file in the bucket directory.
 */
public class PutObjectAction extends S3ActionBase<Void> {

    /**
     * Constructs a PutObjectAction.
     */
    public PutObjectAction() {
    }

    @Override
    public Void handle(AWSRequest request) {
        String bucketName = request.getParams().getFirst("BucketName");
        String objectName = request.getParams().getFirst("ObjectName");
        Optional.ofNullable(getS3Directory())
                .map(dir -> Path.of(dir.getPath(), bucketName, objectName))
                .ifPresentOrElse(path -> {
                    try {
                        Files.write(path, request.getBody().readAllBytes());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }, () -> {
                    try {
                        request.getBody().close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

        return null;
    }
}
