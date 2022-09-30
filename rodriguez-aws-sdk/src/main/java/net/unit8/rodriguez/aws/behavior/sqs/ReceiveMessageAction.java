package net.unit8.rodriguez.aws.behavior.sqs;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.util.Md5Utils;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReceiveMessageAction implements MockAction<ReceiveMessageAction.ReceiveMessageResponse> {
    @Override
    public ReceiveMessageResponse handle(AWSRequest params) {
        ReceiveMessageResponse response = new ReceiveMessageResponse();
        response.ResponseMetadata = new ResponseMetadata(Map.of(
                ResponseMetadata.AWS_REQUEST_ID, UUID.randomUUID().toString()
        ));
        response.ReceiveMessageResult = new ReceiveMessageResult();
        response.ReceiveMessageResult.Message = new ArrayList<>();
        Message message = new Message();
        message.Body = "This is a testMessage";
        message.MD5OfBody = new BigInteger(1, Md5Utils.computeMD5Hash(
                message.Body.getBytes(StandardCharsets.UTF_8)))
                .toString(16);
        response.ReceiveMessageResult.Message.add(message);
        return response;
    }

    public static class ReceiveMessageResponse implements Serializable {
        public ReceiveMessageResult ReceiveMessageResult;
        public ResponseMetadata ResponseMetadata;
    }

    public static class ReceiveMessageResult implements Serializable {
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<Message> Message;
    }

    public static class Message implements Serializable {
        public String MessageId;
        public String ReceiptHandle;
        public String MD5OfBody;
        public String Body;
    }
}
