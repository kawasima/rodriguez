package net.unit8.rodriguez.jdbc.impl;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

/**
 * A timer that introduces configurable delays and detects query timeouts.
 *
 * <p>Used by the mock database to simulate slow responses and enforce query timeout behavior.</p>
 */
public class DelayTimer {
    private final Clock clock = Clock.systemDefaultZone();
    private final long startTime;
    private final int timeout;

    /**
     * Constructs a new {@code DelayTimer} with the given timeout.
     *
     * @param timeout the query timeout in milliseconds; 0 or negative means no timeout
     */
    public DelayTimer(int timeout) {
        startTime = clock.millis();
        this.timeout = timeout;
    }

    /**
     * Sleeps for the specified duration and checks whether the timeout has been exceeded.
     *
     * @param millis the delay duration in milliseconds
     * @return {@code true} if the timeout has been exceeded, {@code false} otherwise
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
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
