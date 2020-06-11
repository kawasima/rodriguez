package net.unit8.rodriguez;

import net.unit8.rodriguez.metrics.MetricRegistry;

import java.util.concurrent.atomic.AtomicReference;

public interface MetricsAvailable {
    AtomicReference<MetricRegistry> _METRIC_REGISTRY_HOLDER = new AtomicReference<>();

    default void setMetricRegistry(MetricRegistry metricRegistry) {
        _METRIC_REGISTRY_HOLDER.set(metricRegistry);
    }

    default MetricRegistry getMetricRegistry() {
        return _METRIC_REGISTRY_HOLDER.get();
    }
}
