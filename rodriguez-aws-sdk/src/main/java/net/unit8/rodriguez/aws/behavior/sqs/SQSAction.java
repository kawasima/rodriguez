package net.unit8.rodriguez.aws.behavior.sqs;

import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

/**
 * Enumeration of SQS API actions, each delegating to a corresponding {@link MockAction} handler.
 */
public enum SQSAction {
    /** Handles the SQS CreateQueue API operation. */
    CreateQueue(new CreateQueueAction()),
    /** Handles the SQS DeleteQueue API operation. */
    DeleteQueue(new DeleteQueueAction()),
    /** Handles the SQS GetQueueUrl API operation. */
    GetQueueUrl(new GetQueueUrlAction()),
    /** Handles the SQS SendMessage API operation. */
    SendMessage(new SendMessageAction()),
    /** Handles the SQS DeleteMessage API operation. */
    DeleteMessage(new DeleteMessageAction()),
    /** Handles the SQS ReceiveMessage API operation. */
    ReceiveMessage(new ReceiveMessageAction())
    ;

    SQSAction(MockAction<?> mockAction) {
        this.mockAction = mockAction;
    }

    private final MockAction<?> mockAction;

    /**
     * Delegates the request to the underlying mock action handler.
     *
     * @param request the incoming AWS request
     * @param <T> the response type
     * @return the response produced by the action handler
     */
    public <T> T handle(AWSRequest request) {
        return (T) mockAction.handle(request);
    }
}
