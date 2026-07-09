package net.unit8.rodriguez.gcp;

/**
 * Signals a client-side error while handling a GCS mock request
 * (e.g. an invalid object/bucket name or an oversized request body).
 *
 * <p>The {@link #getStatusCode() status code} is rendered by
 * {@link net.unit8.rodriguez.gcp.behavior.GCSMock} into a GCS JSON API style
 * error response.
 */
public class GCSException extends RuntimeException {
    private final int statusCode;

    /**
     * Creates a new GCSException.
     *
     * @param statusCode the HTTP status code to return to the client
     * @param message    a human-readable error message
     */
    public GCSException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code associated with this error.
     *
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return statusCode;
    }
}
