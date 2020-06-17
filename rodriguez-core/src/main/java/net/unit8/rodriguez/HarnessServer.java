package net.unit8.rodriguez;

import net.unit8.rodriguez.configuration.ConfigParser;
import net.unit8.rodriguez.configuration.HarnessConfig;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HarnessServer {
    private final HarnessConfig config;
    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final Object lock = new Object();
    private boolean terminated;

    private List<Runnable> servers;
    private ControlServer controlServer;
    private ExecutorService executor;

    public HarnessServer() {
        try (InputStream is = HarnessConfig.class.getClassLoader().getResourceAsStream("META-INF/rodriguez/default-config.json")) {
            ConfigParser parser = new ConfigParser();
            config = parser.parse(is);
        } catch (IOException e) {
            throw new IllegalStateException("Can't start with a default configuration", e);
        }
    }

    public HarnessServer(HarnessConfig config) {
        this.config = config;
    }

    public Runnable createServer(InstabilityBehavior behavior, int port) {
        if (behavior instanceof MetricsAvailable) {
            ((MetricsAvailable) behavior).setMetricRegistry(metricRegistry);
        }
        if (behavior.canListen()) {
            return behavior.createServer(executor, port);
        }
        return () -> {};
    }

    public void start() {
        start(Executors.newCachedThreadPool());
    }

    public void start(ExecutorService executor) {
        config.getControlPort().ifPresent(p -> controlServer = new ControlServer(p, this));

        this.executor = executor;
        servers = config.getPorts()
                .entrySet()
                .stream()
                .map(entry -> createServer(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
    }

    public void await() throws InterruptedException {
        synchronized (lock) {
            while(!terminated) {
                lock.wait();
            }
        }
    }

    public HarnessConfig getConfig() {
        return this.config;
    }

    public MetricRegistry getMetricRegistry() {
        return this.metricRegistry;
    }

    public void shutdown() {
        synchronized (lock) {
            if (executor != null) {
                executor.shutdown();
            }
            if (servers != null) {
                servers.forEach(Runnable::run);
            }

            if (controlServer != null) {
                controlServer.shutdown();
            }
            terminated = true;
            lock.notifyAll();
        }
    }
}
