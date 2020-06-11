package net.unit8.rodriguez.strategy;

import net.unit8.rodriguez.SocketInstabilityStrategy;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NeverDrain implements SocketInstabilityStrategy {
    private static final Logger LOG = Logger.getLogger(NeverDrain.class.getName());

    @Override
    public void handle(Socket socket) throws InterruptedException {
        try {
            socket.setReceiveBufferSize(64 * 1024);
            TimeUnit.DAYS.sleep(1);
        } catch(SocketException e) {
            LOG.log(Level.SEVERE, "Receive Buffer size", e);
        }
        try {
            socket.close();
        } catch(IOException e) {
            LOG.log(Level.SEVERE, "Close error", e);
        }
    }
}
