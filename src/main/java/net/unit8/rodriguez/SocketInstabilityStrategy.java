package net.unit8.rodriguez;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;

public interface SocketInstabilityStrategy extends InstabilityStrategy<ServerSocket> {
    default boolean canAccept() {
        return true;
    }


    @Override
    default ServerSocket createServer(Executor executor, int port) {
        try {
            ServerSocket server = new ServerSocket(port);

            executor.execute(() -> {
                boolean interrupted = false;
                while (!interrupted) {
                    try (Socket socket = server.accept()) {
                        handle(socket);
                    } catch (InterruptedException e) {
                        interrupted = true;
                    } catch (IOException e) {

                    }
                }
            });

            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    default void handle(Socket socket) throws InterruptedException {

    }

}
