package net.unit8.rodriguez.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import dev.failsafe.Failsafe;
import dev.failsafe.Timeout;
import dev.failsafe.TimeoutExceededException;
import net.unit8.rodriguez.HarnessServer;
import net.unit8.rodriguez.aws.behavior.S3Mock;
import net.unit8.rodriguez.behavior.SlowResponse;
import net.unit8.rodriguez.configuration.HarnessConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class S3ScenarioTest {
    AmazonS3 s3client;
    AmazonS3 s3clientSlowResponse;
    HarnessServer server;

    @BeforeEach
    void setUpClient() throws IOException {
        s3client = AmazonS3ClientBuilder.standard()
                .withCredentials(new DummyCredentialsProvider())
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://127.0.0.1:10201",
                                "nishiogi"
                        )
                )
                .build();
        s3clientSlowResponse = AmazonS3ClientBuilder.standard()
                .withClientConfiguration(new ClientConfiguration()
                        .withRequestTimeout(3000))
                .withCredentials(new DummyCredentialsProvider())
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://127.0.0.1:10203",
                                "nishiogi"
                        )
                ).withPathStyleAccessEnabled(true)
                .build();
        HarnessConfig config = new HarnessConfig();
        S3Mock s3Mock = new S3Mock();
        File directory = Files.createTempDirectory("rodriguez").toFile();
        s3Mock.setS3Directory(directory);
        config.setPorts(Map.of(
                10201, s3Mock,
                10203, new SlowResponse()
        ));
        server = new HarnessServer(config);
        server.start();
    }

    @Test
    void testSuccessfully() throws IOException {
        Bucket bucket = s3client.createBucket("my-bucket");
        PutObjectResult putResult = s3client.putObject(new PutObjectRequest(bucket.getName(), "test.txt", new File("src/test/resources/test.txt")));
        ObjectListing objects = s3client.listObjects(bucket.getName());
        assertThat(objects.getObjectSummaries()).hasSize(1);
        S3Object object = s3client.getObject(bucket.getName(), "test.txt");
        String content = new String(object.getObjectContent().readAllBytes(), StandardCharsets.UTF_8);
        s3client.deleteObject(bucket.getName(), "test.txt");
        assertThat(s3client.listObjects(bucket.getName()).getObjectSummaries())
                .hasSize(0);
        s3client.deleteBucket("my-bucket");
    }

    @Test
    void testSlowResponse() throws IOException {
        Bucket bucket = s3client.createBucket("my-bucket");
        PutObjectResult putResult = s3client.putObject(new PutObjectRequest(bucket.getName(), "test.txt", new File("src/test/resources/test.txt")));
        ObjectListing objects = s3client.listObjects(bucket.getName());
        assertThat(objects.getObjectSummaries()).hasSize(1);
        Timeout<Object> timeoutPolicy = Timeout.builder(Duration.ofSeconds(5))
                .withInterrupt()
                .build();

        assertThatThrownBy(() -> Failsafe.with(timeoutPolicy)
                .get(() -> {
                    // getObject reads first some bytes.
                    // So request timeout doesn't work.
                    S3Object object = s3clientSlowResponse.getObject(bucket.getName(), "test.txt");
                    return new String(object.getObjectContent().readAllBytes(), StandardCharsets.UTF_8);
                })).isInstanceOf(TimeoutExceededException.class);
    }
    @AfterEach
    void tearDown() {
        if (server != null) {
            server.shutdown();
        }
    }
}
