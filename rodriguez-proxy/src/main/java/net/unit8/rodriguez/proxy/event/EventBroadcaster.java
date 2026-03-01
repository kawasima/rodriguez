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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Broadcasts fault rule events to connected SSE clients.
 *
 * <p>Implements {@link FaultRuleStore.FaultRuleListener} to receive events
 * from the store and forward them as SSE messages.
 */
public class EventBroadcaster implements FaultRuleStore.FaultRuleListener, ObservedPathStore.Observer {
    private static final Logger LOG = Logger.getLogger(EventBroadcaster.class.getName());
    private final CopyOnWriteArrayList<OutputStream> clients = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Registers a new SSE client output stream.
     *
     * @param os the client's response output stream
     */
    public void addClient(OutputStream os) {
        clients.add(os);
    }

    private void broadcast(FaultEvent event) {
        String json;
        try {
            json = mapper.writeValueAsString(event);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to serialize event", e);
            return;
        }
        String sseMessage = "event: " + event.type() + "\ndata: " + json + "\n\n";
        byte[] bytes = sseMessage.getBytes(StandardCharsets.UTF_8);

        for (OutputStream os : clients) {
            try {
                os.write(bytes);
                os.flush();
            } catch (IOException e) {
                clients.remove(os);
            }
        }
    }

    private void broadcastRaw(String eventType, String json) {
        String sseMessage = "event: " + eventType + "\ndata: " + json + "\n\n";
        byte[] bytes = sseMessage.getBytes(StandardCharsets.UTF_8);
        for (OutputStream os : clients) {
            try {
                os.write(bytes);
                os.flush();
            } catch (IOException e) {
                clients.remove(os);
            }
        }
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
     * Closes all connected SSE clients.
     */
    public void shutdown() {
        for (OutputStream os : clients) {
            try {
                os.close();
            } catch (IOException ignore) {
                // Client already disconnected
            }
        }
        clients.clear();
    }
}
