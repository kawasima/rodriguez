package net.unit8.rodriguez.proxy.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.unit8.rodriguez.proxy.model.FaultEvent;
import net.unit8.rodriguez.proxy.model.FaultRule;
import net.unit8.rodriguez.proxy.store.FaultRuleStore;
import net.unit8.rodriguez.proxy.store.ObservedPathStore;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Broadcasts fault rule events to connected SSE clients.
 *
 * <p>Implements {@link FaultRuleStore.FaultRuleListener} to receive events
 * from the store and forward them as SSE messages.
 *
 * <p>Each client owns a bounded queue and a dedicated writer thread. Broadcasting only
 * offers bytes to those queues (never blocks on a socket write), so a single slow or
 * stalled SSE reader can neither block the broadcasting thread nor other clients, and can
 * never grow memory without bound: when its queue overflows the client is dropped. A
 * client's own writer drains its queue in FIFO order, keeping that client's SSE framing
 * intact.
 */
public class EventBroadcaster implements FaultRuleStore.FaultRuleListener, ObservedPathStore.Observer {
    private static final Logger LOG = Logger.getLogger(EventBroadcaster.class.getName());
    private static final int HEARTBEAT_INTERVAL_SECONDS = 30;
    /** Per-client queue capacity. When full, the client is treated as a stalled reader and dropped. */
    private static final int CLIENT_QUEUE_CAPACITY = 1024;
    /** Upper bound on concurrent SSE clients, so a client opening connections in a loop
     * cannot spawn unbounded writer threads and queues. */
    private static final int MAX_CLIENTS = 256;

    private final CopyOnWriteArrayList<Client> clients = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile boolean shuttingDown = false;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    /** Creates a new EventBroadcaster and starts the SSE heartbeat. */
    public EventBroadcaster() {
        scheduler.scheduleAtFixedRate(
                this::sendHeartbeat,
                HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Registers a new SSE client output stream.
     *
     * @param os the client's response output stream
     */
    public void addClient(OutputStream os) {
        if (shuttingDown || clients.size() >= MAX_CLIENTS) {
            if (!shuttingDown) {
                LOG.warning("Rejecting SSE client: reached the maximum of " + MAX_CLIENTS
                        + " concurrent clients");
            }
            try {
                os.close();
            } catch (IOException ignore) {
                // Already gone.
            }
            return;
        }
        Client client = new Client(os);
        clients.add(client);
        client.start();
    }

    private void sendHeartbeat() {
        dispatch(": keep-alive\n\n".getBytes(StandardCharsets.UTF_8));
    }

    private void broadcast(FaultEvent event) {
        String json;
        try {
            json = mapper.writeValueAsString(event);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to serialize event", e);
            return;
        }
        broadcastRaw(event.type(), json);
    }

    private void broadcastRaw(String eventType, String json) {
        String sseMessage = "event: " + eventType + "\ndata: " + json + "\n\n";
        dispatch(sseMessage.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Offers the given bytes to every connected client's queue. This never blocks: a client
     * whose queue is full (a stalled reader) is dropped instead of stalling the broadcast.
     */
    private void dispatch(byte[] bytes) {
        for (Client client : clients) {
            if (!client.enqueue(bytes)) {
                LOG.info("Dropping slow SSE client (queue overflow)");
                dropClient(client);
            }
        }
    }

    private void dropClient(Client client) {
        clients.remove(client);
        client.close();
    }

    @Override
    public void onRuleAdded(FaultRule rule) {
        broadcast(new FaultEvent("rule-added", rule.getId(), rule.getFaultType(),
                rule.getPathPattern(), rule.getRemaining()));
    }

    @Override
    public void onRuleConsumed(FaultRule rule, int remaining) {
        broadcast(new FaultEvent("rule-consumed", rule.getId(), rule.getFaultType(),
                rule.getPathPattern(), remaining));
    }

    @Override
    public void onRuleRemoved(FaultRule rule) {
        broadcast(new FaultEvent("rule-removed", rule.getId(), rule.getFaultType(),
                rule.getPathPattern(), 0));
    }

    @Override
    public void onPathObserved(String path) {
        try {
            String json = mapper.writeValueAsString(Map.of("path", path));
            broadcastRaw("path-observed", json);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to serialize path-observed event", e);
        }
    }

    /**
     * Closes all connected SSE clients and stops the heartbeat scheduler.
     */
    public void shutdown() {
        shuttingDown = true;
        scheduler.shutdownNow();
        for (Client client : clients) {
            client.close();
        }
        clients.clear();
    }

    /**
     * A single connected SSE client. Owns a bounded FIFO queue and a dedicated daemon writer
     * thread that drains the queue to the client's socket. Writes for one client never touch
     * another client's socket, so one stalled reader cannot affect the others.
     */
    private final class Client {
        private final OutputStream os;
        private final BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(CLIENT_QUEUE_CAPACITY);
        private final Thread writer;
        private volatile boolean closed = false;

        Client(OutputStream os) {
            this.os = os;
            this.writer = new Thread(this::run, "sse-writer");
            this.writer.setDaemon(true);
        }

        void start() {
            writer.start();
        }

        /**
         * Offers bytes to this client's queue without blocking.
         *
         * @return {@code false} if the client is closed or its queue is full (stalled reader)
         */
        boolean enqueue(byte[] bytes) {
            if (closed) {
                return false;
            }
            return queue.offer(bytes);
        }

        private void run() {
            try {
                while (!closed) {
                    byte[] bytes = queue.take();
                    os.write(bytes);
                    os.flush();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                // Reader disconnected; fall through to cleanup.
            } finally {
                // Ensure a client that failed on its own (e.g. reader closed) is unregistered.
                clients.remove(this);
                close();
            }
        }

        void close() {
            closed = true;
            writer.interrupt();
            try {
                os.close();
            } catch (IOException ignore) {
                // Client already disconnected.
            }
        }
    }
}
