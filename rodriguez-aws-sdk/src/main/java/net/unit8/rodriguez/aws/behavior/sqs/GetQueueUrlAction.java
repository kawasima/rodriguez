package net.unit8.rodriguez.aws.behavior.sqs;

import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;
import net.unit8.rodriguez.aws.RequestParams;

import java.io.Serializable;

public class GetQueueUrlAction implements MockAction<GetQueueUrlAction.GetQueueUrlResponse> {
    @Override
    public GetQueueUrlResponse handle(AWSRequest request) {
        GetQueueUrlResponse response = new GetQueueUrlResponse();
        response.GetQueueUrlResult = new GetQueueUrlResult();
        response.GetQueueUrlResult.QueueUrl = request.getRequestURI().toASCIIString();
        return response;
    }

    public static class GetQueueUrlResponse implements Serializable {
        public GetQueueUrlResult GetQueueUrlResult;
    }
    public static class GetQueueUrlResult implements Serializable {
        public String QueueUrl;
    }
}
