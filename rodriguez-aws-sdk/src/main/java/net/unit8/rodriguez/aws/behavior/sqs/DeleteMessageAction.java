package net.unit8.rodriguez.aws.behavior.sqs;

import com.amazonaws.ResponseMetadata;
import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public class DeleteMessageAction implements MockAction<DeleteMessageAction.DeleteMessageResponse> {
    @Override
    public DeleteMessageResponse handle(AWSRequest params) {
        DeleteMessageResponse response = new DeleteMessageResponse();
        response.ResponseMetaData = new ResponseMetadata(Map.of(
                ResponseMetadata.AWS_REQUEST_ID, UUID.randomUUID().toString()
        ));
        return response;
    }

    public static class DeleteMessageResponse implements Serializable {
        public ResponseMetadata ResponseMetaData;
    }
}
