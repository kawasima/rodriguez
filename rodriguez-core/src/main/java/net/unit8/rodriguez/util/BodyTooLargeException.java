package net.unit8.rodriguez.util;

/**
 * Thrown by {@link BoundedBody} when a request body exceeds the configured limit.
 *
 * <p>Each module maps this to its own protocol-specific error response (S3 403/413,
 * GCS 413, proxy 413); keeping the overflow detection in one place means the
 * bounded-read arithmetic cannot silently diverge between them.
 */
public class BodyTooLargeException extends RuntimeException {

    private final long limit;

    /**
     * Constructs a {@code BodyTooLargeException} for a body that exceeded {@code limit}.
     *
     * @param limit the maximum number of bytes that was allowed
     */
    public BodyTooLargeException(long limit) {
        super("Request body exceeds the maximum allowed size of " + limit + " bytes");
        this.limit = limit;
    }

    /**
     * Returns the byte limit that was exceeded.
     *
     * @return the maximum number of bytes that was allowed
     */
    public long getLimit() {
        return limit;
    }
}
