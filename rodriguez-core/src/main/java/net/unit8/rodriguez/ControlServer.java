package net.unit8.rodriguez;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.unit8.rodriguez.configuration.HarnessConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ControlServer implements HttpHandler {
    private static final Logger LOG = Logger.getLogger(ControlServer.class.getName());

    private final HttpServer server;
    private final HarnessServer harnessServer;
    private final ObjectMapper mapper;

    public ControlServer(int port, HarnessServer harnessServer) {
        mapper = new ObjectMapper()
                .registerModule(new Jdk8Module());
        this.harnessServer = harnessServer;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", this);
            server.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            ControlMethod controlMethod = ControlMethod.of(path, method);
            switch (controlMethod) {
                case SHUTDOWN:
                    exchange.sendResponseHeaders(200, 0L);
                    new Thread(harnessServer::shutdown).start();
                    break;
                case CONFIG: {
                    byte[] body = mapper.writerFor(HarnessConfig.class).writeValueAsBytes(harnessServer.getConfig());
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    break;
                }
                case METRICS: {
                    byte[] body = mapper.writeValueAsBytes(harnessServer.getMetricRegistry().getMetrics());
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    break;
                }
            }
        } catch (NoSuchElementException e) {
            exchange.sendResponseHeaders(404, 0L);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "control server error", e);
        } finally {
            exchange.close();
        }
    }

    public void shutdown() {
        server.stop(0);
    }

    private enum ControlMethod {
        CONFIG("/config", "GET"),
        METRICS("/metrics", "GET"),
        SHUTDOWN("/shutdown", "POST");

        ControlMethod(String path, String method) {
            this.path = path;
            this.method = method;
        }
        private final String path;
        private final String method;

        static ControlMethod of(String path, String method) {
            return Arrays.stream(values())
                    .filter(e -> e.method.equalsIgnoreCase(method) && e.path.equals(path))
                    .findAny()
                    .orElseThrow();
        }

    }
}
