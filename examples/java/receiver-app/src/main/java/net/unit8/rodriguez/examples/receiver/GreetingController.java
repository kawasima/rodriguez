package net.unit8.rodriguez.examples.receiver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class GreetingController {
    @GetMapping("/api/greet")
    public GreetingResponse greet(@RequestParam(defaultValue = "world") String name) {
        return new GreetingResponse(
                "hello, " + name + "!",
                "receiver-app",
                Instant.now().toString()
        );
    }

    public record GreetingResponse(String message, String app, String timestamp) {
    }
}
