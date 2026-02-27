package net.unit8.rodriguez.metrics;

import java.util.concurrent.atomic.LongAdder;

/**
 * A thread-safe counter metric backed by a {@link LongAdder}.
 */
public class Counter implements Metric {
    private final LongAdder count;

    /**
     * Creates a new counter initialized to zero.
     */
    public Counter() {
        this.count = new LongAdder();
    }

    /**
     * Increments the counter by one.
     */
    public void inc() {
        inc(1);
    }

    /**
     * Increments the counter by the given amount.
     *
     * @param n the amount to increment by
     */
    public void inc(long n) {
        count.add(n);
    }

    /**
     * Returns the current count.
     *
     * @return the current counter value
     */
    public long getCount() {
        return count.sum();
    }
}
