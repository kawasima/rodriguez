package net.unit8.rodriguez.proxy.model;

/**
 * Metadata about an available fault behavior for the proxy UI.
 *
 * @param name        behavior name (e.g., "SlowResponse")
 * @param port        default Rodriguez port for this behavior
 * @param description human-readable description
 */
public record BehaviorInfo(
        String name,
        int port,
        String description
) {
}
