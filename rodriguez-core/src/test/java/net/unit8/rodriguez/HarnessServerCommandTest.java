package net.unit8.rodriguez;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import net.unit8.rodriguez.configuration.HarnessConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class HarnessServerCommandTest {
    final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new Jdk8Module());

    @Test
    void test() throws InterruptedException, IOException {
        String[] args = {
                "-c", "src/test/resources/config-1.json"
        };
        ExecutorService executor = Executors.newCachedThreadPool();

        Future<Integer> exitCodeFuture = executor.submit(() -> new CommandLine(new HarnessServerCommand()).execute(args));
        TimeUnit.SECONDS.sleep(3);
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request configRequest = new Request.Builder()
                .url("http://localhost:10200/config")
                .get()
                .build();
        try (Response configResponse = client.newCall(configRequest).execute()) {
            HarnessConfig config = mapper
                    .readerFor(HarnessConfig.class)
                    .readValue(Objects.requireNonNull(configResponse.body()).byteStream());
            assertThat(config.getPorts()).isEmpty();
        }

        Request shutdownRequest = new Request.Builder()
                .url("http://localhost:10200/shutdown")
                .post(RequestBody.create(new byte[0]))
                .build();
        try (Response response = client.newCall(shutdownRequest).execute()) {
            assertThat(response.isSuccessful()).isTrue();
        }
    }
}