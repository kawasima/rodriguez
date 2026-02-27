package net.unit8.rodriguez;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

/**
 * An instability behavior that operates at the HTTP level.
 *
 * <p>Implementations use {@link com.sun.net.httpserver.HttpServer} and apply fault
 * injection logic in the {@link #handle(HttpExchange)} method.
 */
public interface HttpInstabilityBehavior extends InstabilityBehavior, MetricsAvailable {
    @Override
    default Runnable createServer(Executor executor, int port) {
        InetSocketAddress address = new InetSocketAddress(port);
        try {
            HttpServer httpServer = HttpServer.create(address, 0);
            httpServer.setExecutor(executor);
            httpServer.createContext("/", exchange -> {
                try {
                    getMetricRegistry().counter(MetricRegistry.name(getClass(), "call")).inc();
                    getInstance().handle(exchange);
                } catch (InterruptedException e) {
                    httpServer.stop(0);
                }
            });
            httpServer.start();
            return () -> httpServer.stop(0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns the behavior instance used to handle HTTP exchanges.
     *
     * <p>Subclasses may override this to return a shared or prototype instance.
     *
     * @return the {@link HttpInstabilityBehavior} instance
     */
    @JsonIgnore
    default HttpInstabilityBehavior getInstance() {
        return this;
    }

    /**
     * Handles an HTTP exchange with fault injection logic.
     *
     * @param exchange the HTTP exchange to handle
     * @throws InterruptedException if the handling thread is interrupted
     */
    default void handle(HttpExchange exchange) throws InterruptedException {
        try {
            exchange.sendResponseHeaders(404, 0L);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
