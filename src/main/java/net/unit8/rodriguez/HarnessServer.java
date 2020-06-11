package net.unit8.rodriguez;

import net.unit8.rodriguez.configuration.ConfigParser;
import net.unit8.rodriguez.configuration.HarnessConfig;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

public class HarnessServer {
    private final HarnessConfig config;
    private final MetricRegistry metricRegistry = new MetricRegistry();

    private ControlServer controlServer;
    private ExecutorService executor;

    public HarnessServer() {
        try (InputStream is = HarnessConfig.class.getResourceAsStream("/META-INF/rodriguez/default-config.json")) {
            ConfigParser parser = new ConfigParser();
            config = parser.parse(is);
        } catch (IOException e) {
            throw new IllegalStateException("Can't start with a default configuration", e);
        }
    }

    public HarnessServer(HarnessConfig config) {
        this.config = config;
    }

    public <SERVER> SERVER createServer(InstabilityStrategy<SERVER> strategy, int port) {
        SERVER server = null;
        if (strategy instanceof MetricsAvailable) {
            ((MetricsAvailable) strategy).setMetricRegistry(metricRegistry);
        }
        if (strategy.canListen()) {
            server = strategy.createServer(executor, port);
        }
        return server;
    }

    public void start(ExecutorService executor) {
        config.getControlPort().ifPresent(p -> {
            controlServer = new ControlServer(p, this);
        });

        this.executor = executor;
        config.getPorts().forEach((key, value) -> executor.execute(() -> createServer(value, key)));
    }

    public HarnessConfig getConfig() {
        return this.config;
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }

        if (controlServer != null) {
            controlServer.shutdown();
        }
    }
}
