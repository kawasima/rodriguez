package net.unit8.rodriguez.behavior;

import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.SocketInstabilityBehavior;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A socket behavior that waits for a configurable delay and then sends a TCP RST.
 *
 * <p>After accepting a connection, this behavior sets {@code SO_LINGER} to zero and
 * waits before closing the socket, which causes a TCP RST to be sent instead of a
 * graceful FIN, simulating an abrupt connection reset.
 */
public class NoResponseAndSendRST implements SocketInstabilityBehavior, MetricsAvailable {
    private static final Logger LOG = Logger.getLogger(NoResponseAndSendRST.class.getName());

    /**
     * Creates a new {@code NoResponseAndSendRST} behavior instance.
     */
    public NoResponseAndSendRST() {
    }

    private long delay = 5000;

    @Override
    public void handle(Socket socket) throws InterruptedException {
        try {
            socket.setSoLinger(true, 0);
        } catch(SocketException e) {
            getMetricRegistry().counter(MetricRegistry.name(getClass(), "other-error")).inc();
            throw new UncheckedIOException(e);
        }
        TimeUnit.MILLISECONDS.sleep(delay);
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch(IOException e) {
            getMetricRegistry().counter(MetricRegistry.name(getClass(), "other-error")).inc();
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the delay in milliseconds before sending the RST.
     *
     * @return the delay in milliseconds
     */
    public long getDelay() {
        return delay;
    }

    /**
     * Sets the delay in milliseconds before sending the RST.
     *
     * @param delay the delay in milliseconds
     */
    public void setDelay(long delay) {
        this.delay = delay;
    }
}
