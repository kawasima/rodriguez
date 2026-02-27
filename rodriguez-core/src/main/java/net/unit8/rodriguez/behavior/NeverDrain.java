package net.unit8.rodriguez.behavior;

import net.unit8.rodriguez.SocketInstabilityBehavior;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A socket behavior that accepts a connection but never reads from the receive buffer.
 *
 * <p>After accepting a connection, this behavior sets a small receive buffer and then
 * sleeps indefinitely, causing the client's send buffer to fill up and block.
 */
public class NeverDrain implements SocketInstabilityBehavior {
    private static final Logger LOG = Logger.getLogger(NeverDrain.class.getName());

    /**
     * Creates a new {@code NeverDrain} behavior instance.
     */
    public NeverDrain() {
    }

    @Override
    public void handle(Socket socket) throws InterruptedException {
        try {
            socket.setReceiveBufferSize(64 * 1024);
            TimeUnit.DAYS.sleep(1);
            getMetricRegistry().counter(MetricRegistry.name(getClass(), "handle-complete")).inc();
        } catch(SocketException e) {
            getMetricRegistry().counter(MetricRegistry.name(getClass(), "other-error")).inc();
        } finally {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                getMetricRegistry().counter(MetricRegistry.name(getClass(), "other-error")).inc();
            }
        }
    }
}
