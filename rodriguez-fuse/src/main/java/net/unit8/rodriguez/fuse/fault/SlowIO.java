package net.unit8.rodriguez.fuse.fault;

import java.util.concurrent.TimeUnit;

/**
 * A FUSE fault that simulates slow I/O by introducing a configurable delay.
 *
 * <p>Before returning the normal result, this fault sleeps for the specified duration,
 * simulating high-latency storage or overloaded I/O subsystems.
 */
public class SlowIO implements FuseFault {
    private long delayMs = 5000;

    /**
     * Constructs a new {@code SlowIO} fault with the default delay of 5000 milliseconds.
     */
    public SlowIO() {
    }

    /**
     * Constructs a new {@code SlowIO} fault with the specified delay.
     *
     * @param delayMs the delay in milliseconds to introduce before returning
     */
    public SlowIO(long delayMs) {
        this.delayMs = delayMs;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sleeps for the configured delay before returning the normal result unchanged.
     *
     * @param normalResult the normal result value of the FUSE operation
     * @return the normal result unchanged, after the delay
     */
    @Override
    public int apply(int normalResult) {
        try {
            TimeUnit.MILLISECONDS.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return normalResult;
    }

    /**
     * Returns the delay in milliseconds.
     *
     * @return the delay in milliseconds
     */
    public long getDelayMs() {
        return delayMs;
    }

    /**
     * Sets the delay in milliseconds.
     *
     * @param delayMs the delay in milliseconds to set
     */
    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }
}
