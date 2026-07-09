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
    private String allowedOrigin = "http://localhost:10220";
    private long maxRequestBodyBytes = 10L * 1024 * 1024;
    private int maxObservedPaths = 5000;
    private List<Integer> allowedFaultPorts;

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

    /**
     * Returns the allowed CORS origin for the proxy API and SSE endpoints.
     *
     * <p>Note: the default value is {@code "http://localhost:10220"} regardless of the configured
     * {@code port}. If you change the port, set this value explicitly to match
     * (e.g., {@code "http://localhost:<port>"}).
     *
     * @return allowed origin (default "http://localhost:10220")
     */
    public String getAllowedOrigin() {
        return allowedOrigin;
    }

    /**
     * Sets the allowed CORS origin for the proxy API and SSE endpoints.
     *
     * @param allowedOrigin allowed origin (e.g., "http://localhost:3000")
     */
    public void setAllowedOrigin(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    /**
     * Returns the maximum allowed request body size in bytes.
     *
     * @return max body size (default 10 MB)
     */
    public long getMaxRequestBodyBytes() {
        return maxRequestBodyBytes;
    }

    /**
     * Sets the maximum allowed request body size in bytes.
     *
     * @param maxRequestBodyBytes max body size in bytes
     */
    public void setMaxRequestBodyBytes(long maxRequestBodyBytes) {
        this.maxRequestBodyBytes = maxRequestBodyBytes;
    }

    /**
     * Returns the maximum number of observed paths retained in memory.
     *
     * <p>The observed-path store is bounded to this size; when full, the oldest
     * entries are evicted. This prevents unbounded memory growth from
     * attacker-controlled request paths.
     *
     * @return max observed paths (default 5000)
     */
    public int getMaxObservedPaths() {
        return maxObservedPaths;
    }

    /**
     * Sets the maximum number of observed paths retained in memory.
     *
     * @param maxObservedPaths max observed paths (must be positive)
     */
    public void setMaxObservedPaths(int maxObservedPaths) {
        this.maxObservedPaths = maxObservedPaths;
    }

    /**
     * Returns the explicit allow-list of fault ports a client may target, or
     * {@code null}/empty if not configured.
     *
     * <p>When configured, a client-supplied {@code faultPort} is accepted only
     * if it appears in this list. When not configured, the proxy falls back to
     * the set of behavior ports advertised by the control API. This prevents the
     * proxy from being used as an arbitrary localhost-port relay (SSRF).
     *
     * @return the allowed fault ports, or null if not configured
     */
    public List<Integer> getAllowedFaultPorts() {
        return allowedFaultPorts;
    }

    /**
     * Sets the explicit allow-list of fault ports a client may target.
     *
     * @param allowedFaultPorts the allowed fault ports, or null to defer to the
     *                          control API's advertised behavior ports
     */
    public void setAllowedFaultPorts(List<Integer> allowedFaultPorts) {
        this.allowedFaultPorts = allowedFaultPorts;
    }

    /**
     * Returns whether an explicit allow-list of fault ports has been configured.
     *
     * @return true if {@link #getAllowedFaultPorts()} is non-null and non-empty
     */
    public boolean hasAllowedFaultPorts() {
        return allowedFaultPorts != null && !allowedFaultPorts.isEmpty();
    }

    /**
     * Returns whether the given port is permitted by the configured allow-list.
     *
     * <p>If no explicit allow-list is configured this returns {@code false};
     * callers should then fall back to the control API's advertised ports.
     *
     * @param port the candidate fault port
     * @return true if an explicit allow-list is configured and contains the port
     */
    public boolean isFaultPortAllowed(int port) {
        return hasAllowedFaultPorts() && allowedFaultPorts.contains(port);
    }
}
