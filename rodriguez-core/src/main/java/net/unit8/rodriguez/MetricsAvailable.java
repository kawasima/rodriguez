package net.unit8.rodriguez;

import com.fasterxml.jackson.annotation.JsonIgnore;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Mixin interface that provides access to a shared {@link MetricRegistry}.
 *
 * <p>Behaviors implementing this interface will have the metric registry injected
 * by {@link HarnessServer} before the server starts.
 */
public interface MetricsAvailable {
    /** Shared holder for the {@link MetricRegistry} instance. */
    AtomicReference<MetricRegistry> _METRIC_REGISTRY_HOLDER = new AtomicReference<>();

    /**
     * Sets the shared metric registry.
     *
     * @param metricRegistry the metric registry to use
     */
    default void setMetricRegistry(MetricRegistry metricRegistry) {
        _METRIC_REGISTRY_HOLDER.set(metricRegistry);
    }

    /**
     * Returns the shared metric registry.
     *
     * @return the current {@link MetricRegistry}
     */
    @JsonIgnore
    default MetricRegistry getMetricRegistry() {
        return _METRIC_REGISTRY_HOLDER.get();
    }
}
