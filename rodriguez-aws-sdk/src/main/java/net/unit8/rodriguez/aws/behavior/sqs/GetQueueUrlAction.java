package net.unit8.rodriguez.aws.behavior.sqs;

import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

import java.util.Map;

/**
 * Handles the SQS GetQueueUrl API operation by returning the queue URL derived from the request URI.
 */
public class GetQueueUrlAction implements MockAction<Map<String, String>> {

    /**
     * Constructs a GetQueueUrlAction.
     */
    public GetQueueUrlAction() {
    }

    @Override
    public Map<String, String> handle(AWSRequest request) {
        return Map.of("QueueUrl", request.getRequestURI().toASCIIString());
    }
}
