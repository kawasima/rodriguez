package net.unit8.rodriguez.proxy;

import net.unit8.rodriguez.proxy.store.ObservedPathStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObservedPathStoreTest {

    @Test
    void recordsNewPathsAndNotifiesObserverOnce() {
        ObservedPathStore store = new ObservedPathStore(10);
        List<String> observed = new ArrayList<>();
        store.addObserver(observed::add);

        assertThat(store.record("/a")).isTrue();
        assertThat(store.record("/a")).isFalse();
        assertThat(store.record("/b")).isTrue();

        assertThat(store.getPaths()).containsExactly("/a", "/b");
        assertThat(observed).containsExactly("/a", "/b");
    }

    @Test
    void evictsOldestWhenOverCapacity() {
        ObservedPathStore store = new ObservedPathStore(3);

        store.record("/1");
        store.record("/2");
        store.record("/3");
        store.record("/4"); // evicts /1

        assertThat(store.getPaths()).containsExactly("/2", "/3", "/4");
        assertThat(store.getPaths()).doesNotContain("/1");
    }

    @Test
    void neverExceedsMaxSizeUnderManyDistinctPaths() {
        ObservedPathStore store = new ObservedPathStore(100);
        for (int i = 0; i < 10_000; i++) {
            store.record("/path/" + i);
        }
        assertThat(store.getPaths()).hasSize(100);
        // The most recent 100 paths are retained.
        assertThat(store.getPaths()).contains("/path/9999", "/path/9900");
    }

    @Test
    void rejectsNonPositiveMaxSize() {
        assertThatThrownBy(() -> new ObservedPathStore(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
