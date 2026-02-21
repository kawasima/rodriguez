package net.unit8.rodriguez.aws.behavior.sqs;

import com.amazonaws.util.Md5Utils;
import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class SendMessageAction implements MockAction<Map<String, String>> {
    private static final AtomicLong sequenceNumber = new AtomicLong(1);

    @Override
    public Map<String, String> handle(AWSRequest request) {
        String messageBody = request.getParams().getFirst("MessageBody");
        String md5 = new BigInteger(1, Md5Utils.computeMD5Hash(
                messageBody.getBytes(StandardCharsets.UTF_8)))
                .toString(16);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("MD5OfMessageBody", md5);
        response.put("MessageId", UUID.randomUUID().toString());
        response.put("SequenceNumber", Long.toString(sequenceNumber.addAndGet(1)));
        return response;
    }
}
