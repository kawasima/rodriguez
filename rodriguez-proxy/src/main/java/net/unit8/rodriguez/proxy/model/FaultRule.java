package net.unit8.rodriguez.proxy.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A fault injection rule that matches request paths and routes them to a Rodriguez fault port.
 *
 * <p>Each rule has a countdown ({@code remaining}) that decrements on each match.
 * When the count reaches zero, the rule is automatically removed by the store.
 * An optional {@code duration} causes the rule to expire after the specified time.
 */
public class FaultRule {
    private static final Pattern DURATION_SHORTHAND = Pattern.compile("(\\d+)([smh])");

    private final String id;
    private final String pathPattern;
    private final Pattern compiledPattern;
    private final String faultType;
    private final int faultPort;
    private final AtomicInteger remaining;
    private final Instant createdAt;
    private final Duration duration;

    /**
     * Creates a new fault rule.
     *
     * @param pathPattern regex pattern to match request paths
     * @param faultType   behavior name (e.g., "SlowResponse")
     * @param faultPort   Rodriguez port to forward to (e.g., 10205)
     * @param count       number of requests to inject the fault
     */
    public FaultRule(String pathPattern, String faultType, int faultPort, int count) {
        this(pathPattern, faultType, faultPort, count, null);
    }

    /**
     * Creates a new fault rule with an optional TTL duration.
     *
     * @param pathPattern    regex pattern to match request paths
     * @param faultType      behavior name (e.g., "SlowResponse")
     * @param faultPort      Rodriguez port to forward to (e.g., 10205)
     * @param count          number of requests to inject the fault
     * @param durationString TTL in shorthand format (e.g., "30s", "5m", "1h"), or null for no expiry
     */
    public FaultRule(String pathPattern, String faultType, int faultPort, int count, String durationString) {
        this.id = UUID.randomUUID().toString();
        this.pathPattern = pathPattern;
        this.compiledPattern = Pattern.compile(pathPattern);
        this.faultType = faultType;
        this.faultPort = faultPort;
        this.remaining = new AtomicInteger(count);
        this.createdAt = Instant.now();
        this.duration = parseDuration(durationString);
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

    /**
     * Returns the TTL duration, or null if no expiry is set.
     *
     * @return the duration, or null
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * Returns whether this rule has expired based on its TTL duration.
     *
     * @return true if the rule has a duration set and has expired
     */
    public boolean isExpired() {
        return duration != null && Instant.now().isAfter(createdAt.plus(duration));
    }

    private static Duration parseDuration(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        Matcher m = DURATION_SHORTHAND.matcher(s);
        if (m.matches()) {
            long value = Long.parseLong(m.group(1));
            return switch (m.group(2)) {
                case "s" -> Duration.ofSeconds(value);
                case "m" -> Duration.ofMinutes(value);
                case "h" -> Duration.ofHours(value);
                default -> Duration.parse(s);
            };
        }
        try {
            return Duration.parse(s);
        } catch (java.time.format.DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid duration: " + s, e);
        }
    }
}
