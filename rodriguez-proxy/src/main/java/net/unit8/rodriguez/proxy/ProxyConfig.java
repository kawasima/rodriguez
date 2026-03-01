package net.unit8.rodriguez.proxy;

import java.util.List;

/**
 * Configuration for the fault-injecting reverse proxy extension.
 */
public class ProxyConfig {
    private int port = 10220;
    private String upstream;
    private List<String> paths;
    private int connectTimeoutMs = 5000;
    private int requestTimeoutMs = 30000;
    private String controlUrl = "http://localhost:10200";

    /** Creates a new ProxyConfig with default values. */
    public ProxyConfig() {
    }

    /**
     * Returns the port the proxy listens on.
     *
     * @return proxy listen port (default 10220)
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port the proxy listens on.
     *
     * @param port proxy listen port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the upstream service URL to forward requests to.
     *
     * @return upstream URL
     */
    public String getUpstream() {
        return upstream;
    }

    /**
     * Sets the upstream service URL.
     *
     * @param upstream upstream URL (e.g., "http://localhost:8080")
     */
    public void setUpstream(String upstream) {
        this.upstream = upstream;
    }

    /**
     * Returns the list of path prefixes displayed in the UI.
     *
     * @return path prefixes, or null if not configured
     */
    public List<String> getPaths() {
        return paths;
    }

    /**
     * Sets the list of path prefixes for the UI.
     *
     * @param paths path prefixes
     */
    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    /**
     * Returns the connection timeout in milliseconds.
     *
     * @return connection timeout (default 5000)
     */
    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    /**
     * Sets the connection timeout in milliseconds.
     *
     * @param connectTimeoutMs connection timeout
     */
    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    /**
     * Returns the request timeout in milliseconds.
     *
     * @return request timeout (default 30000)
     */
    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    /**
     * Sets the request timeout in milliseconds.
     *
     * @param requestTimeoutMs request timeout
     */
    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public String getControlUrl() {
        return controlUrl;
    }

    public void setControlUrl(String controlUrl) {
        this.controlUrl = controlUrl;
    }
}
