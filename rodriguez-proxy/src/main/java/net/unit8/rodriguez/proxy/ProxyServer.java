package net.unit8.rodriguez.proxy;

import com.sun.net.httpserver.HttpServer;
import net.unit8.rodriguez.proxy.event.EventBroadcaster;
import net.unit8.rodriguez.proxy.handler.*;
import net.unit8.rodriguez.proxy.store.FaultRuleStore;
import net.unit8.rodriguez.proxy.store.ObservedPathStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * The fault-injecting reverse proxy server.
 *
 * <p>Creates a single {@link HttpServer} that handles reverse proxying,
 * fault rule management API, SSE events, and UI static file serving.
 */
public class ProxyServer {
    private static final Logger LOG = Logger.getLogger(ProxyServer.class.getName());

    private final ProxyConfig config;
    private HttpServer httpServer;
    private EventBroadcaster broadcaster;

    /**
     * Creates a new proxy server.
     *
     * @param config proxy configuration
     */
    public ProxyServer(ProxyConfig config) {
        this.config = config;
    }

    /**
     * Starts the proxy server, creating all handlers and listeners.
     */
    public void start() {
        FaultRuleStore store = new FaultRuleStore();
        ObservedPathStore observedPathStore = new ObservedPathStore();
        broadcaster = new EventBroadcaster();
        store.addListener(broadcaster);
        observedPathStore.addObserver(broadcaster);

        HttpClient httpClient = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        try {
            httpServer = HttpServer.create(new InetSocketAddress(config.getPort()), 0);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create proxy server on port " + config.getPort(), e);
        }
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        httpServer.createContext("/_proxy/api/", new ApiHandler(store, config, observedPathStore));
        httpServer.createContext("/_proxy/events", new EventStreamHandler(broadcaster));
        httpServer.createContext("/_proxy/ui/", new StaticFileHandler());
        httpServer.createContext("/", new ProxyHandler(httpClient, store, config, observedPathStore));

        httpServer.start();
        LOG.info("Proxy server started on port " + config.getPort()
                + " -> upstream " + config.getUpstream());
    }

    /**
     * Stops the proxy server and disconnects all SSE clients.
     */
    public void shutdown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (broadcaster != null) {
            broadcaster.shutdown();
        }
    }
}
