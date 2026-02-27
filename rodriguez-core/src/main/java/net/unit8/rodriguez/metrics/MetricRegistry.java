package net.unit8.rodriguez.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A registry of named {@link Metric} instances.
 *
 * <p>Provides thread-safe registration and retrieval of metrics such as {@link Counter}.
 */
public class MetricRegistry {
    private final ConcurrentMap<String, Metric> metrics;

    /**
     * Builds a dot-separated metric name from the given components.
     *
     * @param name  the first component of the metric name
     * @param names additional components to append, separated by dots
     * @return the assembled metric name
     */
    public static String name(String name, String... names) {
        final StringBuilder builder = new StringBuilder();
        append(builder, name);
        if (names != null) {
            for (String s : names) {
                append(builder, s);
            }
        }
        return builder.toString();
    }
    /**
     * Builds a dot-separated metric name using a class name as the first component.
     *
     * @param klass the class whose fully qualified name is used as the prefix
     * @param names additional components to append, separated by dots
     * @return the assembled metric name
     */
    public static String name(Class<?> klass, String... names) {
        return name(klass.getName(), names);
    }

    private static void append(StringBuilder builder, String part) {
        if (part != null && !part.isEmpty()) {
            if (builder.length() > 0) {
                builder.append('.');
            }
            builder.append(part);
        }
    }
    /**
     * Creates a new empty metric registry.
     */
    public MetricRegistry() {
        metrics = new ConcurrentHashMap<>();
    }

    /**
     * Registers a metric under the given name.
     *
     * <p>If a metric with the same name already exists, the existing metric is kept
     * but the new metric is still returned.
     *
     * @param <T>    the type of the metric
     * @param name   the name to register the metric under
     * @param metric the metric instance to register
     * @return the given metric instance
     */
    public <T extends Metric> T register(String name, T metric) {
         final Metric existing = metrics.putIfAbsent(name, metric);
         return metric;
    }

    /**
     * Returns the {@link Counter} registered under the given name, creating one if absent.
     *
     * @param name the counter name
     * @return the existing or newly created counter
     */
    public Counter counter(String name) {
        return getOrAdd(name, MetricBuilder.COUNTERS);
    }

    /**
     * Returns an unmodifiable view of all registered metrics.
     *
     * @return a map of metric names to metric instances
     */
    public Map<String, Metric> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }

    @SuppressWarnings("unchecked")
    private <T extends Metric> T getOrAdd(String name, MetricBuilder<T> builder) {
        final Metric metric = metrics.get(name);
        if (metric != null) {
            return (T) metric;
        } else {
            return register(name, builder.newMetric());
        }
    }

   private interface MetricBuilder<T extends Metric> {
        MetricBuilder<Counter> COUNTERS = new MetricBuilder<>() {
            @Override
            public Counter newMetric() {
                return new Counter();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return metric instanceof Counter;
            }
        };


        T newMetric();

        boolean isInstance(Metric metric);
    }
}
