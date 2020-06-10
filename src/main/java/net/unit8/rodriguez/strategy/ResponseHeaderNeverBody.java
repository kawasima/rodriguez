package net.unit8.rodriguez.strategy;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityStrategy;

import java.io.IOException;

public class ResponseHeaderNeverBody implements HttpInstabilityStrategy {
    @Override
    public void handle(HttpExchange exchange) throws InterruptedException {
        try {
            exchange.sendResponseHeaders(200, 5000);
        } catch (IOException e) {

        }

    }
}
