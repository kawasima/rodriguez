package net.unit8.rodriguez.proxy.model;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * A fault injection rule that matches request paths and routes them to a Rodriguez fault port.
 *
 * <p>Each rule has a countdown ({@code remaining}) that decrements on each match.
 * When the count reaches zero, the rule is automatically removed by the store.
 */
public class FaultRule {
    private final String id;
    private final String pathPattern;
    private final Pattern compiledPattern;
    private final String faultType;
    private final int faultPort;
    private final AtomicInteger remaining;

    /**
     * Creates a new fault rule.
     *
     * @param pathPattern regex pattern to match request paths
     * @param faultType   behavior name (e.g., "SlowResponse")
     * @param faultPort   Rodriguez port to forward to (e.g., 10205)
     * @param count       number of requests to inject the fault
     */
    public FaultRule(String pathPattern, String faultType, int faultPort, int count) {
        this.id = UUID.randomUUID().toString();
        this.pathPattern = pathPattern;
        this.compiledPattern = Pattern.compile(pathPattern);
        this.faultType = faultType;
        this.faultPort = faultPort;
        this.remaining = new AtomicInteger(count);
    }

    /**
     * Tests whether the given path matches this rule's pattern.
     *
     * @param path the request path to test
     * @return true if the path matches
     */
    public boolean matches(String path) {
        return compiledPattern.matcher(path).matches();
    }

    /**
     * Decrements the remaining count atomically.
     *
     * @return the remaining count after decrement
     */
    public int decrementAndGet() {
        return remaining.decrementAndGet();
    }

    /**
     * Increments the remaining count atomically.
     *
     * @return the remaining count after increment
     */
    public int incrementAndGet() {
        return remaining.incrementAndGet();
    }

    /**
     * Returns the unique identifier for this rule.
     *
     * @return rule ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the regex pattern string.
     *
     * @return path pattern
     */
    public String getPathPattern() {
        return pathPattern;
    }

    /**
     * Returns the fault behavior name.
     *
     * @return fault type name
     */
    public String getFaultType() {
        return faultType;
    }

    /**
     * Returns the Rodriguez port to forward matching requests to.
     *
     * @return fault port number
     */
    public int getFaultPort() {
        return faultPort;
    }

    /**
     * Returns the current remaining request count.
     *
     * @return remaining count
     */
    public int getRemaining() {
        return remaining.get();
    }
}
