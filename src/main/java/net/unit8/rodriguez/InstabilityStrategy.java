package net.unit8.rodriguez;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.net.Socket;
import java.util.concurrent.Executor;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
public interface InstabilityStrategy<SERVER> {
    default boolean canListen() {
        return true;
    }
    SERVER createServer(Executor executor, int port);
}
