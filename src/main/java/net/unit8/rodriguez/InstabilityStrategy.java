package net.unit8.rodriguez;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.concurrent.Executor;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
public interface InstabilityStrategy {
    default boolean canListen() {
        return true;
    }
    Runnable createServer(Executor executor, int port);
}
