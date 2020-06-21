package net.unit8.rodriguez;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;

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

    @JsonIgnore
    default HttpInstabilityBehavior getInstance() {
        return this;
    }

    default void handle(HttpExchange exchange) throws InterruptedException {
        try {
            exchange.sendResponseHeaders(404, 0L);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
