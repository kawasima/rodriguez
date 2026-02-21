package net.unit8.rodriguez.aws.behavior.sqs;

import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

import java.util.Map;

public class CreateQueueAction implements MockAction<Map<String, String>> {
    @Override
    public Map<String, String> handle(AWSRequest request) {
        return Map.of("QueueUrl", request.getRequestURI().toASCIIString());
    }
}
