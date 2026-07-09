package net.unit8.rodriguez;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

/**
 * Base interface for all fault injection behaviors.
 *
 * <p>Each implementation defines a specific instability pattern (e.g., connection refusal,
 * slow response) that can be bound to a port. Uses Jackson {@code @JsonTypeInfo} for
 * polymorphic JSON deserialization based on the {@code type} short name.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@JsonTypeIdResolver(InstabilityBehaviorTypeIdResolver.class)
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
     * <p>Each behavior owns the thread resources it needs for handling connections and
     * releases them when the returned shutdown {@link Runnable} is invoked, so that
     * thread exhaustion on one port cannot affect other ports.
     *
     * @param port the port number to listen on
     * @return a {@link Runnable} that, when invoked, shuts down the created server
     */
    Runnable createServer(int port);
}
