package net.unit8.rodriguez.aws;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectResult;
import net.unit8.rodriguez.HarnessServer;
import net.unit8.rodriguez.aws.behavior.S3Mock;
import net.unit8.rodriguez.configuration.HarnessConfig;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class S3Test {
    static AmazonS3 s3client;
    static HarnessServer server;

    @BeforeAll
    static void setUpClient() {
        s3client = AmazonS3ClientBuilder.standard()
                .withCredentials(new DummyCredentialsProvider())
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://localhost:10202",
                                "nishiogi"
                        )
                )
                .build();
        HarnessConfig config = new HarnessConfig();
        config.setPorts(Map.of(10202, new S3Mock()));
        server = new HarnessServer(config);
        server.start();

    }

    @Test
    void createBucket(){
        Bucket bucket = s3client.createBucket("my-bucket");
        assertThat(bucket.getName()).isEqualTo("my-bucket");
    }

    @Test
    void putObject() {
        PutObjectResult result = s3client.putObject("my-bucket", "key", new File("src/test/resources/test.txt"));
        assertThat(result.getContentMd5()).isNotNull();
    }

    @Test
    void listObjects() {
        ObjectListing objectListing = s3client.listObjects("my-bucket");
    }

    @Test
    void listBuckets() {
        List<Bucket> buckets = s3client.listBuckets();
    }

    @AfterAll
    static void tearDown() {
        server.shutdown();
    }
}
