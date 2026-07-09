package net.unit8.rodriguez.aws;

/**
 * Thrown when an incoming request body exceeds the maximum allowed size.
 *
 * <p>Used to guard the in-memory / streaming reads of the AWS mocks against
 * out-of-memory conditions caused by unbounded uploads.</p>
 */
public class BodyTooLargeException extends RuntimeException {

    /**
     * Constructs a {@code BodyTooLargeException} with the given detail message.
     *
     * @param message the detail message
     */
    public BodyTooLargeException(String message) {
        super(message);
    }
}
