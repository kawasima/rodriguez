package net.unit8.rodriguez.aws.behavior.sqs;

import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

import java.io.Serializable;

public class CreateQueueAction implements MockAction<CreateQueueAction.CreateQueueResponse> {
    @Override
    public CreateQueueResponse handle(AWSRequest request) {
        CreateQueueResponse response = new CreateQueueResponse();
        response.CreateQueueResult = new CreateQueueResult();
        response.CreateQueueResult.QueueUrl = request.getRequestURI().toASCIIString();

        return response;
    }

    public static class CreateQueueResponse implements Serializable {
        public CreateQueueResult CreateQueueResult;
    }

    public static class CreateQueueResult implements Serializable {
        public String QueueUrl;
    }
}
