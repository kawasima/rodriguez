package net.unit8.rodriguez.aws.behavior.sqs;

import com.amazonaws.ResponseMetadata;
import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public class DeleteQueueAction implements MockAction<DeleteQueueAction.DeleteQueueResponse> {
    @Override
    public DeleteQueueResponse handle(AWSRequest params) {
        DeleteQueueResponse response = new DeleteQueueResponse();
        response.ResponseMetadata = new ResponseMetadata(Map.of(
                ResponseMetadata.AWS_REQUEST_ID, UUID.randomUUID().toString()
        ));
        return response;
    }

    public static class DeleteQueueResponse implements Serializable {
        public ResponseMetadata ResponseMetadata;
    }
}
