package net.unit8.rodriguez.aws.behavior.s3;

/**
 * Thrown when a bucket or object name resolves to a filesystem path outside the
 * configured S3 storage root (path traversal), or when a name is otherwise invalid.
 */
public class S3AccessDeniedException extends RuntimeException {

    /**
     * Constructs an {@code S3AccessDeniedException} with the given detail message.
     *
     * @param message the detail message
     */
    public S3AccessDeniedException(String message) {
        super(message);
    }
}
