package net.unit8.rodriguez.proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.unit8.rodriguez.HarnessExtension;

/**
 * Rodriguez extension that provides a fault-injecting reverse proxy.
 *
 * <p>Activated by adding a {@code "proxy"} entry in the {@code "extensions"} config:
 * <pre>{@code
 * {
 *   "extensions": {
 *     "proxy": {
 *       "port": 10220,
 *       "upstream": "http://localhost:8080"
 *     }
 *   }
 * }
 * }</pre>
 */
public class ProxyExtension implements HarnessExtension {
    private ProxyServer server;

    /** Creates a new ProxyExtension. */
    public ProxyExtension() {
    }

    @Override
    public String getName() {
        return "proxy";
    }

    @Override
    public void start(JsonNode config) {
        ObjectMapper mapper = new ObjectMapper();
        ProxyConfig proxyConfig = mapper.convertValue(config, ProxyConfig.class);
        server = new ProxyServer(proxyConfig);
        server.start();
    }

    @Override
    public void shutdown() {
        if (server != null) {
            server.shutdown();
        }
    }
}
