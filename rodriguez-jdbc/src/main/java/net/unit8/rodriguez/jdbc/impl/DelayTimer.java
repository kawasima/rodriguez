package net.unit8.rodriguez.jdbc.impl;

import java.sql.SQLException;
import java.time.Clock;
import java.util.concurrent.TimeUnit;

public class DelayTimer {
    private final Clock clock = Clock.systemDefaultZone();
    private final long startTime;
    private final int timeout;

    public DelayTimer(int timeout) {
        startTime = clock.millis();
        this.timeout = timeout;
    }

    public boolean isTimeout(long millis) throws InterruptedException {
        long currentTime = clock.millis();
        if (timeout > 0 && currentTime - startTime > timeout) {
            return true;
        }
        if (timeout > 0 && (timeout - (currentTime - startTime)) < millis) {
            TimeUnit.MILLISECONDS.sleep(timeout - (currentTime - startTime));
            return true;
        }
        TimeUnit.MILLISECONDS.sleep(millis);
        return false;
    }
}
