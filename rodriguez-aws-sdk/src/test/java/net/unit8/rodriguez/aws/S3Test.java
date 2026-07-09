package net.unit8.rodriguez.aws;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectResult;
import net.unit8.rodriguez.HarnessServer;
import net.unit8.rodriguez.aws.behavior.S3Mock;
import net.unit8.rodriguez.behavior.SlowResponse;
import net.unit8.rodriguez.configuration.HarnessConfig;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class S3Test {
    AmazonS3 s3client;
    HarnessServer server;
    File directory;

    @BeforeEach
    void setUpClient() throws IOException {
        s3client = AmazonS3ClientBuilder.standard()
                .withCredentials(new DummyCredentialsProvider())
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                "http://127.0.0.1:10202",
                                "nishiogi"
                        )
                )
                .withPathStyleAccessEnabled(true)
                .build();
        S3Mock s3mock = new S3Mock();
        directory = Files.createTempDirectory("rodriguez").toFile();
        s3mock.setS3Directory(directory);

        HarnessConfig config = new HarnessConfig();
        config.setPorts(Map.of(
                10202, s3mock,
                10203, new SlowResponse()
        ));
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
        s3client.createBucket("my-bucket");
        PutObjectResult result = s3client.putObject("my-bucket", "key", new File("src/test/resources/test.txt"));
        assertThat(result.getContentMd5()).isNotNull();
    }

    @Test
    void listObjects() {
        s3client.createBucket("my-bucket");
        ObjectListing objectListing = s3client.listObjects("my-bucket");
    }

    @Test
    void listBuckets() {
        s3client.createBucket("my-bucket");
        List<Bucket> buckets = s3client.listBuckets();
    }

    @Test
    void getMissingObjectReturns404() {
        s3client.createBucket("my-bucket");
        assertThatThrownBy(() -> s3client.getObject("my-bucket", "no-such-key"))
                .isInstanceOf(AmazonS3Exception.class)
                .satisfies(e -> assertThat(((AmazonS3Exception) e).getStatusCode()).isEqualTo(404));
    }

    @Test
    void listObjectsRejectsTraversalBucketName() throws Exception {
        s3client.createBucket("my-bucket");
        // A traversing BucketName passed via the query string must not list a
        // directory outside the storage root; it is rejected with 403.
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:10202/?BucketName=..%2F..%2F..%2F..%2Fetc"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.body()).doesNotContain("passwd");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
        if (directory != null) {
            Files.walk(directory.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
