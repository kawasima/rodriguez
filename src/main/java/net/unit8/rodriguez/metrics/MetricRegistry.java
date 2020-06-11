package net.unit8.rodriguez.metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MetricRegistry {
    private final ConcurrentMap<String, Metric> metrics;

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
    public MetricRegistry() {
        metrics = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <T extends Metric> T register(String name, T metric) {
         final Metric existing = metrics.putIfAbsent(name, metric);
         return metric;
    }

    public Counter counter(String name) {
        return getOrAdd(name, MetricBuilder.COUNTERS);
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
        MetricBuilder<Counter> COUNTERS = new MetricBuilder<Counter>() {
            @Override
            public Counter newMetric() {
                return new Counter();
            }

            @Override
            public boolean isInstance(Metric metric) {
                return Counter.class.isInstance(metric);
            }
        };


        T newMetric();

        boolean isInstance(Metric metric);
    }
}
