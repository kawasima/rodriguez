package net.unit8.rodriguez.proxy.store;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe store for paths observed via successful upstream responses (HTTP 200-399).
 *
 * <p>The store is bounded to a maximum size. Request paths are attacker-controlled,
 * so an unbounded store would allow a remote client to exhaust memory (DoS) simply by
 * issuing requests with ever-changing paths. When the store is full, the oldest entry
 * is evicted (insertion-order LRU) before a new one is recorded.
 */
public class ObservedPathStore {
    /** Default maximum number of retained paths. */
    public static final int DEFAULT_MAX_SIZE = 5000;

    private final int maxSize;
    // Insertion-ordered map used as a bounded set; the eldest entry is evicted when full.
    private final LinkedHashMap<String, Boolean> paths = new LinkedHashMap<>();
    private final CopyOnWriteArrayList<Observer> observers = new CopyOnWriteArrayList<>();

    public interface Observer {
        void onPathObserved(String path);
    }

    /** Creates a store bounded to {@link #DEFAULT_MAX_SIZE} entries. */
    public ObservedPathStore() {
        this(DEFAULT_MAX_SIZE);
    }

    /**
     * Creates a store bounded to the given maximum number of entries.
     *
     * @param maxSize maximum number of paths to retain (must be positive)
     */
    public ObservedPathStore(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive: " + maxSize);
        }
        this.maxSize = maxSize;
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    /**
     * Records a path as observed. If the path is new, notifies all observers.
     * When the store is at capacity, the oldest entry is evicted to make room.
     *
     * @param path the request path
     * @return true if the path was newly added
     */
    public boolean record(String path) {
        boolean added;
        synchronized (paths) {
            added = paths.put(path, Boolean.TRUE) == null;
            if (added && paths.size() > maxSize) {
                Iterator<String> it = paths.keySet().iterator();
                it.next();
                it.remove();
            }
        }
        if (added) {
            observers.forEach(o -> o.onPathObserved(path));
        }
        return added;
    }

    public Set<String> getPaths() {
        synchronized (paths) {
            return new LinkedHashSet<>(paths.keySet());
        }
    }
}
