package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.BodyTooLargeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles the S3 PutObject API operation by streaming the request body to a file in the bucket directory.
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
        Path path = resolveObjectPath(bucketName, objectName);
        try (InputStream in = request.getBody();
             OutputStream out = Files.newOutputStream(path)) {
            long total = 0;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > AWSRequest.MAX_BODY_SIZE) {
                    throw new BodyTooLargeException(
                            "Object body exceeds the maximum allowed size of "
                                    + AWSRequest.MAX_BODY_SIZE + " bytes");
                }
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (BodyTooLargeException e) {
            deleteQuietly(path);
            throw e;
        }
        return null;
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignore) {
            // best effort cleanup of the partially written object
        }
    }
}
