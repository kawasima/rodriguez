package net.unit8.rodriguez.strategy;

import com.sun.net.httpserver.HttpExchange;
import net.unit8.rodriguez.HttpInstabilityStrategy;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class SlowResponse implements HttpInstabilityStrategy {
    private long interval = 3000;
    private byte sentChar = 0x20;

    @Override
    public void handle(HttpExchange exchange) throws InterruptedException {
        try {
            exchange.sendResponseHeaders(200, 5000);
            System.out.println("response header");
            OutputStream os = exchange.getResponseBody();
            for (int i=0; i<5000; i++) {
                System.out.println("write");
                os.write(sentChar);
                os.flush();
                TimeUnit.MILLISECONDS.sleep(interval);
            }
        } catch (IOException e) {

        }
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }
}
