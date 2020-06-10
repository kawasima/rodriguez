package net.unit8.rodriguez.strategy;

import net.unit8.rodriguez.SocketInstabilityStrategy;

import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class NoResponseAndSendRST implements SocketInstabilityStrategy {
    @Override
    public void handle(Socket socket) throws InterruptedException {
        TimeUnit.SECONDS.sleep(5);
    }
}
