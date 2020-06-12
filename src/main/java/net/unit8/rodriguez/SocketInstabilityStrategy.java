package net.unit8.rodriguez;

import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public interface SocketInstabilityStrategy extends InstabilityStrategy, MetricsAvailable {
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

    default void handle(Socket socket) throws InterruptedException {

    }
}
