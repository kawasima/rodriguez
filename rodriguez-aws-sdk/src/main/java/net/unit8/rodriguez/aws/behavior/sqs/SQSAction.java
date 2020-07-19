package net.unit8.rodriguez.aws.behavior.sqs;

import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

public enum SQSAction {
    CreateQueue(new CreateQueueAction()),
    DeleteQueue(new DeleteQueueAction()),
    GetQueueUrl(new GetQueueUrlAction()),
    SendMessage(new SendMessageAction()),
    DeleteMessage(new DeleteMessageAction()),
    ReceiveMessage(new ReceiveMessageAction())
    ;

    SQSAction(MockAction<?> mockAction) {
        this.mockAction = mockAction;
    }

    private MockAction<?> mockAction;

    public <T> T handle(AWSRequest request) {
        return (T) mockAction.handle(request);
    }
}
