package net.unit8.rodriguez.aws;

/**
 * Interface for handling mock AWS API actions.
 *
 * <p>Each implementation handles a specific AWS API operation and produces
 * a response of type {@code T}.</p>
 *
 * @param <T> the type of the response produced by this action
 */
public interface MockAction<T> {
    /**
     * Handles an AWS request and produces a response.
     *
     * @param params the incoming AWS request
     * @return the response object, or {@code null} if no response body is needed
     */
    T handle(AWSRequest params);
}
