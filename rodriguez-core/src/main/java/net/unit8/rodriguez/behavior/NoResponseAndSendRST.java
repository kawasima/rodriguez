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

public class NoResponseAndSendRST implements SocketInstabilityBehavior, MetricsAvailable {
    private static final Logger LOG = Logger.getLogger(NoResponseAndSendRST.class.getName());
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

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }
}
