package net.unit8.rodriguez.behavior;

import net.unit8.rodriguez.SocketInstabilityBehavior;

/**
 * A socket behavior that listens on a port but never accepts incoming connections.
 *
 * <p>The server socket is created and bound, but {@code accept()} is never called,
 * causing clients to hang in the TCP connection queue.
 */
public class NotAccept implements SocketInstabilityBehavior {

    /**
     * Creates a new {@code NotAccept} behavior instance.
     */
    public NotAccept() {
    }
    @Override
    public boolean canListen() {
        return true;
    }

    @Override
    public boolean canAccept() {
        return false;
    }
}
