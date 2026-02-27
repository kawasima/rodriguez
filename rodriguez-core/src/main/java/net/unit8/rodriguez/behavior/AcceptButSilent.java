package net.unit8.rodriguez.behavior;

import net.unit8.rodriguez.SocketInstabilityBehavior;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A socket behavior that accepts TCP connections but never sends a response.
 *
 * <p>After accepting a connection and draining the client's request data, this behavior
 * silently waits indefinitely without sending any response, simulating a hung server.
 */
public class AcceptButSilent implements SocketInstabilityBehavior {
    private static final Logger LOG = Logger.getLogger(AcceptButSilent.class.getName());

    /**
     * Creates a new {@code AcceptButSilent} behavior instance.
     */
    public AcceptButSilent() {
    }

    @Override
    public void handle(Socket socket) throws InterruptedException {
        try {
            // Set a short SO_TIMEOUT so we can drain the client's request
            // without blocking indefinitely (HTTP clients keep the connection open
            // waiting for a response, so read() would block forever without a timeout)
            socket.setSoTimeout(5000);
            InputStream is = socket.getInputStream();
            byte[] buf = new byte[4096];
            try {
                while (is.read(buf) != -1) {
                    // consume and discard
                }
            } catch (SocketTimeoutException e) {
                // Client stopped sending — expected for HTTP clients
                // that are now waiting for a response
            }
            // Now silently wait — never send a response
            TimeUnit.DAYS.sleep(1);
            getMetricRegistry().counter(MetricRegistry.name(getClass(), "handle-complete")).inc();
        } catch (IOException e) {
            getMetricRegistry().counter(MetricRegistry.name(getClass(), "client-timeout")).inc();
        }
    }
}
