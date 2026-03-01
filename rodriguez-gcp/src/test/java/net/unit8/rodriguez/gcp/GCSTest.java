package net.unit8.rodriguez.gcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.unit8.rodriguez.HarnessServer;
import net.unit8.rodriguez.gcp.behavior.GCSMock;
import net.unit8.rodriguez.configuration.HarnessConfig;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GCSTest {
    static HarnessServer server;
    static HttpClient httpClient;
    static ObjectMapper mapper;

    static final String BASE_URL = "http://127.0.0.1:10230";
    static final String PROJECT = "test-project";

    @BeforeAll
    static void setUp() {
        mapper = new ObjectMapper();
        httpClient = HttpClient.newHttpClient();

        HarnessConfig config = new HarnessConfig();
        config.setPorts(Map.of(10230, new GCSMock()));
        server = new HarnessServer(config);
        server.start();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) server.shutdown();
    }

    @Test
    void createBucket() throws Exception {
        String body = mapper.writeValueAsString(Map.of("name", "my-bucket"));
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b?project=" + PROJECT))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = mapper.readTree(response.body());
        assertThat(json.get("kind").asText()).isEqualTo("storage#bucket");
        assertThat(json.get("name").asText()).isEqualTo("my-bucket");
    }

    @Test
    void listBuckets() throws Exception {
        // Create a bucket first
        String body = mapper.writeValueAsString(Map.of("name", "list-bucket"));
        httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b?project=" + PROJECT))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b?project=" + PROJECT))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = mapper.readTree(response.body());
        assertThat(json.get("kind").asText()).isEqualTo("storage#buckets");
        assertThat(json.has("items")).isTrue();
    }

    @Test
    void uploadAndGetObject() throws Exception {
        // Create bucket
        String bucketBody = mapper.writeValueAsString(Map.of("name", "upload-bucket"));
        httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b?project=" + PROJECT))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bucketBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        // Upload object
        byte[] content = "hello, gcs!".getBytes(StandardCharsets.UTF_8);
        HttpResponse<String> uploadResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/upload/storage/v1/b/upload-bucket/o?uploadType=media&name=test.txt"))
                        .header("Content-Type", "text/plain")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(uploadResponse.statusCode()).isEqualTo(200);
        JsonNode uploadJson = mapper.readTree(uploadResponse.body());
        assertThat(uploadJson.get("name").asText()).isEqualTo("test.txt");
        assertThat(uploadJson.get("bucket").asText()).isEqualTo("upload-bucket");
        assertThat(uploadJson.get("size").asLong()).isEqualTo(content.length);
        assertThat(uploadJson.has("md5Hash")).isTrue();
        assertThat(uploadJson.has("crc32c")).isTrue();

        // Download object
        HttpResponse<byte[]> getResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b/upload-bucket/o/test.txt?alt=media"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(getResponse.statusCode()).isEqualTo(200);
        assertThat(getResponse.body()).isEqualTo(content);
    }

    @Test
    void getObjectMetadata() throws Exception {
        // Create bucket and upload object
        String bucketBody = mapper.writeValueAsString(Map.of("name", "meta-bucket"));
        httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b?project=" + PROJECT))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bucketBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        byte[] content = "metadata test content".getBytes(StandardCharsets.UTF_8);
        httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/upload/storage/v1/b/meta-bucket/o?uploadType=media&name=meta.txt"))
                        .header("Content-Type", "text/plain")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(content))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        // Get metadata
        HttpResponse<String> metaResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b/meta-bucket/o/meta.txt"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(metaResponse.statusCode()).isEqualTo(200);
        JsonNode json = mapper.readTree(metaResponse.body());
        assertThat(json.get("kind").asText()).isEqualTo("storage#object");
        assertThat(json.get("name").asText()).isEqualTo("meta.txt");
        assertThat(json.get("bucket").asText()).isEqualTo("meta-bucket");
        assertThat(json.get("size").asLong()).isEqualTo(content.length);
        assertThat(json.get("contentType").asText()).isEqualTo("application/octet-stream");
    }

    @Test
    void listObjects() throws Exception {
        // Create bucket and upload objects
        String bucketBody = mapper.writeValueAsString(Map.of("name", "list-obj-bucket"));
        httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b?project=" + PROJECT))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bucketBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        for (String name : new String[]{"a.txt", "b.txt"}) {
            httpClient.send(
                    HttpRequest.newBuilder()
                            .uri(URI.create(BASE_URL + "/upload/storage/v1/b/list-obj-bucket/o?uploadType=media&name=" + name))
                            .header("Content-Type", "text/plain")
                            .POST(HttpRequest.BodyPublishers.ofString("data"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> listResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b/list-obj-bucket/o"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(listResponse.statusCode()).isEqualTo(200);
        JsonNode json = mapper.readTree(listResponse.body());
        assertThat(json.get("kind").asText()).isEqualTo("storage#objects");
        assertThat(json.get("items").size()).isEqualTo(2);
    }

    @Test
    void deleteObject() throws Exception {
        // Create bucket and upload object
        String bucketBody = mapper.writeValueAsString(Map.of("name", "del-obj-bucket"));
        httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b?project=" + PROJECT))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bucketBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/upload/storage/v1/b/del-obj-bucket/o?uploadType=media&name=todel.txt"))
                        .header("Content-Type", "text/plain")
                        .POST(HttpRequest.BodyPublishers.ofString("to be deleted"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        // Delete object
        HttpResponse<String> deleteResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b/del-obj-bucket/o/todel.txt"))
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(deleteResponse.statusCode()).isEqualTo(204);

        // Verify object is gone — items should be an empty array
        HttpResponse<String> listResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b/del-obj-bucket/o"))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        JsonNode json = mapper.readTree(listResponse.body());
        assertThat(json.get("items").size()).isEqualTo(0);
    }

    @Test
    void deleteBucket() throws Exception {
        // Create bucket
        String bucketBody = mapper.writeValueAsString(Map.of("name", "del-bucket"));
        httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b?project=" + PROJECT))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(bucketBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        // Delete bucket
        HttpResponse<String> deleteResponse = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b/del-bucket"))
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(deleteResponse.statusCode()).isEqualTo(204);
    }

    @Test
    void unsupportedMethodReturns404() throws Exception {
        // PUT is not mapped to any GCSAction, so it should return 404
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/storage/v1/b/any-bucket"))
                        .PUT(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(404);
    }
}
