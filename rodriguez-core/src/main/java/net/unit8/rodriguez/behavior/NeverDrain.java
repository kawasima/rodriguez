package net.unit8.rodriguez.behavior;

import net.unit8.rodriguez.SocketInstabilityBehavior;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class NeverDrain implements SocketInstabilityBehavior {
    private static final Logger LOG = Logger.getLogger(NeverDrain.class.getName());

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
