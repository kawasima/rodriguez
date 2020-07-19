package net.unit8.rodriguez.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;
import net.unit8.rodriguez.HarnessServer;
import net.unit8.rodriguez.aws.behavior.SQSMock;
import net.unit8.rodriguez.configuration.HarnessConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SQSTest {
    AmazonSQS sqsClient;
    HarnessServer server;

    @BeforeEach
    void setUpClient() {
        sqsClient = AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://localhost:10201",
                                "nishiogi"
                        )
                )
                .withCredentials(new DummyCredentialsProvider())
                .build();
        HarnessConfig config = new HarnessConfig();
        config.setPorts(Map.of(10201, new SQSMock()));
        server = new HarnessServer(config);
        server.start();

    }

    @Test
    void createQueue() {
        CreateQueueRequest create_request = new CreateQueueRequest("QUEUE")
                .addAttributesEntry("DelaySeconds", "60")
                .addAttributesEntry("MessageRetentionPeriod", "86400");

        CreateQueueResult result = sqsClient.createQueue(create_request);
        assertThat(result.getQueueUrl()).isEqualTo("/");
    }

    @Test
    void sendMessage() {
        String queueUrl = sqsClient.getQueueUrl("QUEUE").getQueueUrl();
        SendMessageRequest send_msg_request = new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageBody("hello world")
            .withDelaySeconds(5);
        SendMessageResult result = sqsClient.sendMessage(send_msg_request);
        assertThat(result.getMD5OfMessageBody()).isNotNull();
    }

    @Test
    void receiveMessage() {
        String queueUrl = sqsClient.getQueueUrl("QUEUE").getQueueUrl();
        ReceiveMessageResult result = sqsClient.receiveMessage(queueUrl);
        assertThat(result.getMessages()).hasSize(1);
    }

    @Test
    void deleteQueue() {
        String queueUrl = sqsClient.getQueueUrl("QUEUE").getQueueUrl();
        DeleteQueueResult result = sqsClient.deleteQueue(queueUrl);
    }
    @Test
    void deleteMessage() {
        String queueUrl = sqsClient.getQueueUrl("QUEUE").getQueueUrl();
        sqsClient.deleteMessage(new DeleteMessageRequest()
                .withQueueUrl(queueUrl)
        );
    }

    @AfterEach
    void tearDown() {
        if (sqsClient != null) {
            sqsClient.shutdown();
        }
        if (server != null) {
            server.shutdown();
        }
    }
}
