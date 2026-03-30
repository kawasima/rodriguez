package net.unit8.rodriguez.behavior;

import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.SocketInstabilityBehavior;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * A socket behavior that performs a TCP half-close.
 *
 * <p>After accepting a connection and optionally sending a partial HTTP response,
 * this behavior calls {@link Socket#shutdownOutput()} to send a FIN in the
 * send direction while keeping the receive direction open. This simulates a
 * server that signals end-of-stream to the client without fully closing the
 * connection.
 *
 * <p>Two variants are supported via {@code sendPartialResponse}:
 * <ul>
 *   <li>{@code false} (default): FIN is sent immediately after draining the request,
 *       with no response body. The client receives an empty response.</li>
 *   <li>{@code true}: HTTP headers are sent before the FIN, but no body follows.
 *       The client receives headers and then EOF, causing JSON parse failures.</li>
 * </ul>
 */
public class HalfClose implements SocketInstabilityBehavior, MetricsAvailable {
    private boolean sendPartialResponse = false;
    private long delayMs = 0;

    /**
     * Creates a new {@code HalfClose} behavior instance.
     */
    public HalfClose() {
    }

    @Override
    public void handle(Socket socket) throws InterruptedException {
        try {
            getMetricRegistry().counter(MetricRegistry.name(getClass(), "call")).inc();

            // Drain the client's request so the client is ready to read the response
            socket.setSoTimeout(3000);
            InputStream is = socket.getInputStream();
            byte[] buf = new byte[4096];
            try {
                while (is.read(buf) != -1) {
                    // consume and discard
                }
            } catch (SocketTimeoutException e) {
                // Client stopped sending — expected for HTTP clients waiting for a response
            }

            // Optionally send HTTP headers without a body
            if (sendPartialResponse) {
                OutputStream os = socket.getOutputStream();
                os.write(("HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "\r\n").getBytes());
                os.flush();
            }

            // Wait before closing, if configured
            if (delayMs > 0) {
                TimeUnit.MILLISECONDS.sleep(delayMs);
            }

            // Half-close: send FIN in the outbound direction only
            socket.shutdownOutput();

            // Keep receive side open — wait for the client to close its side
            TimeUnit.SECONDS.sleep(30);

            getMetricRegistry().counter(MetricRegistry.name(getClass(), "handle-complete")).inc();
        } catch (SocketTimeoutException e) {
            getMetricRegistry().counter(MetricRegistry.name(getClass(), "client-timeout")).inc();
        } catch (IOException e) {
            getMetricRegistry().counter(MetricRegistry.name(getClass(), "other-error")).inc();
        }
    }

    /**
     * Returns whether a partial HTTP response (headers only) is sent before the half-close.
     *
     * @return true if headers are sent before shutting down output
     */
    public boolean isSendPartialResponse() {
        return sendPartialResponse;
    }

    /**
     * Sets whether to send a partial HTTP response (headers only) before the half-close.
     *
     * @param sendPartialResponse true to send headers before shutting down output
     */
    public void setSendPartialResponse(boolean sendPartialResponse) {
        this.sendPartialResponse = sendPartialResponse;
    }

    /**
     * Returns the delay in milliseconds before performing the half-close.
     *
     * @return delay in milliseconds
     */
    public long getDelayMs() {
        return delayMs;
    }

    /**
     * Sets the delay in milliseconds before performing the half-close.
     *
     * @param delayMs delay in milliseconds
     */
    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }
}
