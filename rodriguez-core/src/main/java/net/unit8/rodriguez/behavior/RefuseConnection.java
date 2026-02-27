package net.unit8.rodriguez.behavior;

import net.unit8.rodriguez.SocketInstabilityBehavior;

/**
 * A socket behavior that refuses all TCP connections by not listening on the port.
 *
 * <p>No server socket is created, causing clients to receive a connection refused error.
 */
public class RefuseConnection implements SocketInstabilityBehavior {

    /**
     * Creates a new {@code RefuseConnection} behavior instance.
     */
    public RefuseConnection() {
    }
    @Override
    public boolean canListen() {
        return false;
    }
}
