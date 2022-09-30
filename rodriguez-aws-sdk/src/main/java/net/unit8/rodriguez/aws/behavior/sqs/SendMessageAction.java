package net.unit8.rodriguez.aws.behavior.sqs;

import com.amazonaws.util.Md5Utils;
import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class SendMessageAction implements MockAction<SendMessageAction.SendMessageResponse> {
    private static final AtomicLong sequenceNumber = new AtomicLong(1);

    @Override
    public SendMessageResponse handle(AWSRequest request) {
        SendMessageResponse response = new SendMessageResponse();
        response.SendMessageResult = new SendMessageResult();
        response.SendMessageResult.MD5OfMessageBody = new BigInteger(1, Md5Utils.computeMD5Hash(
                request.getParams().getFirst("MessageBody").getBytes(StandardCharsets.UTF_8)))
                .toString(16);
        response.SendMessageResult.MessageId = UUID.randomUUID().toString();
        response.SendMessageResult.SequenceNumber = Long.toString(sequenceNumber.addAndGet(1));
        return response;
    }

    static class SendMessageResult implements Serializable {
        public String MD5OfMessageAttributes;
        public String MD5OfMessageBody;
        public String MD5OfMessageSystemAttributes;
        public String MessageId;
        public String SequenceNumber;
    }
    static class SendMessageResponse implements Serializable {
        public SendMessageResult SendMessageResult;

    }
}
