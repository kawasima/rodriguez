package net.unit8.rodriguez.metrics;

import java.util.concurrent.atomic.LongAdder;

public class Counter implements Metric {
    private final LongAdder count;

    public Counter() {
        this.count = new LongAdder();
    }

    public void inc() {
        inc(1);
    }

    public void inc(long n) {
        count.add(n);
    }

    public long getCount() {
        return count.sum();
    }
}
