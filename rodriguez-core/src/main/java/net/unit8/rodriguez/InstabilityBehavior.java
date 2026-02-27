package net.unit8.rodriguez;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.concurrent.Executor;

/**
 * Base interface for all fault injection behaviors.
 *
 * <p>Each implementation defines a specific instability pattern (e.g., connection refusal,
 * slow response) that can be bound to a port. Uses Jackson {@code @JsonTypeInfo} for
 * polymorphic JSON deserialization based on the {@code type} class name.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
public interface InstabilityBehavior {
    /**
     * Returns whether this behavior should create a listening server socket.
     *
     * @return {@code true} if this behavior listens on a port; {@code false} otherwise
     */
    default boolean canListen() {
        return true;
    }

    /**
     * Creates and starts a server that exhibits this instability behavior on the given port.
     *
     * @param executor the executor to use for handling connections
     * @param port     the port number to listen on
     * @return a {@link Runnable} that, when invoked, shuts down the created server
     */
    Runnable createServer(Executor executor, int port);
}
