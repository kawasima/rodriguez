package net.unit8.rodriguez.proxy.store;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe store for paths observed via successful upstream responses (HTTP 200-399).
 */
public class ObservedPathStore {
    private final Set<String> paths = ConcurrentHashMap.newKeySet();
    private final List<Observer> observers = new CopyOnWriteArrayList<>();

    public interface Observer {
        void onPathObserved(String path);
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    /**
     * Records a path as observed. If the path is new, notifies all observers.
     *
     * @param path the request path
     * @return true if the path was newly added
     */
    public boolean record(String path) {
        if (paths.add(path)) {
            observers.forEach(o -> o.onPathObserved(path));
            return true;
        }
        return false;
    }

    public Set<String> getPaths() {
        return Collections.unmodifiableSet(paths);
    }
}
