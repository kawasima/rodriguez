package net.unit8.rodriguez.examples.caller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
public class CallController {
    private final RestClient restClient;
    private final String receiverBaseUrl;

    public CallController(RestClient.Builder restClientBuilder,
                          @Value("${app.receiver.base-url}") String receiverBaseUrl) {
        this.receiverBaseUrl = receiverBaseUrl;
        this.restClient = restClientBuilder.baseUrl(receiverBaseUrl).build();
    }

    @GetMapping("/api/call")
    public CallResponse call(@RequestParam(defaultValue = "world") String name) {
        ReceiverResponse receiverResponse = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/greet").queryParam("name", name).build())
                .retrieve()
                .body(ReceiverResponse.class);

        return new CallResponse(
                "caller-app",
                receiverBaseUrl,
                receiverResponse
        );
    }

    public record CallResponse(String app, String viaBaseUrl, ReceiverResponse receiverResponse) {
    }

    public record ReceiverResponse(String message, String app, String timestamp) {
    }
}
