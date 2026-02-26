package net.unit8.rodriguez;

import com.fasterxml.jackson.databind.JsonNode;

public interface FuseSupport {
    void start(JsonNode config);
    void shutdown();
}
