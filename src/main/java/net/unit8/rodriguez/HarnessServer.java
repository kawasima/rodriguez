package net.unit8.rodriguez;

import net.unit8.rodriguez.configuration.HarnessConfig;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class HarnessServer {
    private Map<Integer, InstabilityStrategy<?>> portMap;

    public HarnessServer(HarnessConfig config) {
        this.portMap = config.getPorts();
    }

    public <SERVER> SERVER createServer(Executor executor, InstabilityStrategy<SERVER> strategy, int port) {
        SERVER server = null;
        if (strategy.canListen()) {
            server = strategy.createServer(executor, port);
        }

        return server;
    }

    public void start(Executor executor) {
        portMap.entrySet()
                .forEach(entry -> {
                    executor.execute(() -> createServer(executor, entry.getValue(), entry.getKey()));
                });
    }
}
