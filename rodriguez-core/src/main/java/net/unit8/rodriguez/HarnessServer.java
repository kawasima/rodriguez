package net.unit8.rodriguez;

import net.unit8.rodriguez.configuration.ConfigParser;
import net.unit8.rodriguez.configuration.HarnessConfig;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Main server that orchestrates fault injection behaviors across multiple ports.
 *
 * <p>Loads default configuration from classpath resources, starts behavior servers
 * on configured ports, and manages extensions via {@link java.util.ServiceLoader}.
 */
public class HarnessServer {
    private static final Logger LOG = Logger.getLogger(HarnessServer.class.getName());
    private final HarnessConfig config;
    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final Object lock = new Object();
    private boolean terminated;

    private List<Runnable> servers;
    private ControlServer controlServer;
    private ExecutorService executor;
    private final List<HarnessExtension> activeExtensions = new ArrayList<>();

    /**
     * Creates a harness server with default configuration loaded from the classpath.
     *
     * <p>Discovers and merges all {@code META-INF/rodriguez/default-config.json}
     * resources from the classpath.
     */
    public HarnessServer() {
        ConfigParser parser = new ConfigParser();
        config = new HarnessConfig();
        try {
            Enumeration<URL> configs = Thread.currentThread().getContextClassLoader().getResources("META-INF/rodriguez/default-config.json");
            while(configs.hasMoreElements()) {
                URL url = configs.nextElement();
                LOG.info("load config:" + url);
                try(InputStream is = url.openStream()) {
                    config.merge(parser.parse(is));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can't start with a default configuration", e);
        }
    }

    /**
     * Creates a harness server with the given configuration.
     *
     * @param config the harness configuration
     */
    public HarnessServer(HarnessConfig config) {
        this.config = config;
    }

    /**
     * Creates a server for the given behavior on the specified port.
     *
     * <p>If the behavior implements {@link MetricsAvailable}, the metric registry
     * is injected before the server is created.
     *
     * @param behavior the instability behavior to serve
     * @param port     the port number to bind to
     * @return a {@link Runnable} that shuts down the created server when invoked
     */
    public Runnable createServer(InstabilityBehavior behavior, int port) {
        if (behavior instanceof MetricsAvailable available) {
            available.setMetricRegistry(metricRegistry);
        }
        if (behavior.canListen()) {
            return behavior.createServer(executor, port);
        }
        return () -> {};
    }

    /**
     * Starts the harness server using a cached thread pool executor.
     */
    public void start() {
        start(Executors.newCachedThreadPool());
    }

    /**
     * Starts the harness server using the given executor service.
     *
     * <p>Creates behavior servers on all configured ports, starts the control server
     * if configured, and initializes any registered extensions.
     *
     * @param executor the executor service to use for handling connections
     */
    public void start(ExecutorService executor) {
        config.getControlPort().ifPresent(p -> controlServer = new ControlServer(p, this));

        this.executor = executor;
        servers = config.getPorts()
                .entrySet()
                .stream()
                .map(entry -> createServer(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());

        Map<String, HarnessExtension> extensionMap = new HashMap<>();
        ServiceLoader.load(HarnessExtension.class).forEach(ext -> extensionMap.put(ext.getName(), ext));

        config.getExtensions().forEach((name, extConfig) -> {
            HarnessExtension ext = extensionMap.get(name);
            if (ext != null) {
                try {
                    ext.start(extConfig);
                    activeExtensions.add(ext);
                    LOG.info("Extension '" + name + "' enabled");
                } catch (UnsatisfiedLinkError e) {
                    LOG.warning("Extension '" + name + "' unavailable (native library not found). Skipping.");
                }
            } else {
                LOG.warning("No extension found for '" + name + "'. Skipping.");
            }
        });

        LOG.info("rodriguez server has started");
    }

    /**
     * Blocks the calling thread until the server is shut down.
     *
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public void await() throws InterruptedException {
        synchronized (lock) {
            while(!terminated) {
                lock.wait();
            }
        }
    }

    /**
     * Returns the harness configuration.
     *
     * @return the current {@link HarnessConfig}
     */
    public HarnessConfig getConfig() {
        return this.config;
    }

    /**
     * Returns the metric registry used to track behavior invocations.
     *
     * @return the {@link MetricRegistry}
     */
    public MetricRegistry getMetricRegistry() {
        return this.metricRegistry;
    }

    /**
     * Shuts down the harness server, stopping all behavior servers, extensions,
     * and the control server.
     */
    public void shutdown() {
        LOG.info("shutdown");
        synchronized (lock) {
            if (executor != null) {
                executor.shutdown();
            }
            if (servers != null) {
                servers.forEach(Runnable::run);
            }

            activeExtensions.forEach(HarnessExtension::shutdown);

            if (controlServer != null) {
                controlServer.shutdown();
            }
            terminated = true;
            lock.notifyAll();
        }
    }
}
