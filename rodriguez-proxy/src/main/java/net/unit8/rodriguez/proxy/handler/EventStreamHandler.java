package net.unit8.rodriguez.proxy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.unit8.rodriguez.proxy.event.EventBroadcaster;

import java.io.IOException;
import java.io.OutputStream;

/**
 * SSE (Server-Sent Events) endpoint handler.
 *
 * <p>Registers the client's output stream with the {@link EventBroadcaster}
 * and keeps the connection open for event delivery.
 */
public class EventStreamHandler implements HttpHandler {
    private final EventBroadcaster broadcaster;

    /**
     * Creates a new event stream handler.
     *
     * @param broadcaster the event broadcaster to register clients with
     */
    public EventStreamHandler(EventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0);

        OutputStream os = exchange.getResponseBody();
        broadcaster.addClient(os);
        // Connection stays open; the broadcaster writes events as they occur.
        // The exchange is NOT closed here — it remains open for SSE streaming.
    }
}
