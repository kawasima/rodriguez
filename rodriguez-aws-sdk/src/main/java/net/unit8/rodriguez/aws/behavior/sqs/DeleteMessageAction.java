package net.unit8.rodriguez.aws.behavior.sqs;

import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

/**
 * Handles the SQS DeleteMessage API operation.
 *
 * <p>This mock implementation is a no-op that always succeeds.</p>
 */
public class DeleteMessageAction implements MockAction<Object> {

    /**
     * Constructs a DeleteMessageAction.
     */
    public DeleteMessageAction() {
    }

    @Override
    public Object handle(AWSRequest params) {
        return null;
    }
}
