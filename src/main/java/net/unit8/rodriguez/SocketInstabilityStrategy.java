package net.unit8.rodriguez;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;

public interface SocketInstabilityStrategy extends InstabilityStrategy {
    default boolean canAccept() {
        return true;
    }

    @Override
    default Runnable createServer(Executor executor, int port) {
        try {
            ServerSocket server = new ServerSocket(port);
            Thread serverThread = new Thread(() -> {
                while (!Thread.interrupted()) {
                    try (Socket socket = server.accept()) {
                        executor.execute(() -> {
                            try {
                                handle(socket);
                            } catch (InterruptedException ie) {

                            }
                        });
                    } catch (IOException e) {
                        if (!server.isClosed()) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            serverThread.start();
            return () -> {
                try {
                    server.close();
                } catch (IOException e) {

                }
                serverThread.interrupt();
            };
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    default void handle(Socket socket) throws InterruptedException {

    }
}
