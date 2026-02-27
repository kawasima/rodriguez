package net.unit8.rodriguez;

import com.fasterxml.jackson.databind.JsonNode;

public interface HarnessExtension {
    String getName();
    void start(JsonNode config);
    void shutdown();
}
