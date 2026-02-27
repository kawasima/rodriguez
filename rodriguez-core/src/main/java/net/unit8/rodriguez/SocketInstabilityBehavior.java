package net.unit8.rodriguez;

import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * An instability behavior that operates at the raw TCP socket level.
 *
 * <p>Implementations use a {@link ServerSocket} to accept connections and apply
 * fault injection logic in the {@link #handle(Socket)} method.
 */
public interface SocketInstabilityBehavior extends InstabilityBehavior, MetricsAvailable {
    /**
     * Returns whether this behavior should accept incoming connections.
     *
     * @return {@code true} if connections should be accepted; {@code false} to block indefinitely
     */
    default boolean canAccept() {
        return true;
    }

    @Override
    default Runnable createServer(Executor executor, int port) {
        try {
            ServerSocket server = new ServerSocket(port);
            Thread serverThread = new Thread(() -> {
                while (!Thread.interrupted()) {
                    if (!canAccept()) {
                        try {
                            TimeUnit.DAYS.sleep(1);
                        } catch(InterruptedException ignore) {

                        }
                    }
                    try {
                        Socket socket = server.accept();
                        executor.execute(() -> {
                            try(socket) {
                                getMetricRegistry().counter(MetricRegistry.name(getClass(), "call")).inc();
                                handle(socket);
                            } catch (InterruptedException ignore) {

                            } catch (IOException e) {
                                getMetricRegistry().counter(MetricRegistry.name(getClass(), "other-error")).inc();
                            }
                        });
                    } catch (IOException e) {
                        if (!server.isClosed()) {
                            getMetricRegistry().counter(MetricRegistry.name(getClass(), "other-error")).inc();
                        }
                    }

                }
            });
            serverThread.start();
            return () -> {
                try {
                    server.close();
                } catch (IOException ignore) {

                }
                serverThread.interrupt();
            };
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Handles an accepted socket connection with fault injection logic.
     *
     * @param socket the accepted client socket
     * @throws InterruptedException if the handling thread is interrupted
     */
    default void handle(Socket socket) throws InterruptedException {

    }
}
