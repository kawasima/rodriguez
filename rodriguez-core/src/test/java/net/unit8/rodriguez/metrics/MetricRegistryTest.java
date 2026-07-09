package net.unit8.rodriguez.metrics;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MetricRegistryTest {

    @Test
    void counterReturnsSameInstanceForSameName() {
        MetricRegistry registry = new MetricRegistry();
        Counter first = registry.counter("foo");
        Counter second = registry.counter("foo");
        assertThat(second).isSameAs(first);
    }

    @Test
    void concurrentFirstTouchSharesSingleCounterAndCountsEveryIncrement() throws Exception {
        final int threads = 32;
        final int incrementsPerThread = 1_000;
        MetricRegistry registry = new MetricRegistry();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        Set<Counter> observed = ConcurrentHashMap.newKeySet();

        try {
            Future<?>[] futures = new Future<?>[threads];
            for (int t = 0; t < threads; t++) {
                futures[t] = pool.submit(() -> {
                    barrier.await();
                    Counter counter = registry.counter("shared");
                    observed.add(counter);
                    for (int i = 0; i < incrementsPerThread; i++) {
                        counter.inc();
                    }
                    return null;
                });
            }
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        // Every thread must have observed the exact same Counter instance,
        // otherwise increments would be silently dropped on the orphaned one.
        assertThat(observed).hasSize(1);
        assertThat(registry.counter("shared").getCount())
                .isEqualTo((long) threads * incrementsPerThread);
    }
}
