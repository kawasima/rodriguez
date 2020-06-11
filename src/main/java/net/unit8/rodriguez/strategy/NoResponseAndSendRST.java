package net.unit8.rodriguez.strategy;

import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.SocketInstabilityStrategy;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NoResponseAndSendRST implements SocketInstabilityStrategy, MetricsAvailable {
    private static final Logger LOG = Logger.getLogger(NoResponseAndSendRST.class.getName());
    private long delay = 5000;

    @Override
    public void handle(Socket socket) throws InterruptedException {
        try {
            socket.setSoLinger(true, 0);
        } catch(SocketException e) {
            LOG.log(Level.SEVERE, "SO_LINGER", e);
        }
        TimeUnit.SECONDS.sleep(delay);
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch(IOException e) {
            LOG.log(Level.SEVERE, "Close error", e);
        }
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }
}
