package net.unit8.rodriguez.aws.behavior.sqs;

import com.amazonaws.util.Md5Utils;
import net.unit8.rodriguez.aws.AWSRequest;
import net.unit8.rodriguez.aws.MockAction;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ReceiveMessageAction implements MockAction<Map<String, Object>> {
    @Override
    public Map<String, Object> handle(AWSRequest params) {
        String body = "This is a testMessage";
        String md5 = new BigInteger(1, Md5Utils.computeMD5Hash(
                body.getBytes(StandardCharsets.UTF_8)))
                .toString(16);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("MessageId", UUID.randomUUID().toString());
        message.put("ReceiptHandle", UUID.randomUUID().toString());
        message.put("MD5OfBody", md5);
        message.put("Body", body);

        return Map.of("Messages", List.of(message));
    }
}
