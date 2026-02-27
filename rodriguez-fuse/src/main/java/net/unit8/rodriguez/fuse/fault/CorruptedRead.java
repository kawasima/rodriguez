package net.unit8.rodriguez.fuse.fault;

import java.util.Random;

/**
 * A FUSE fault that simulates corrupt data being returned on read operations.
 *
 * <p>Rather than returning an error code, this fault randomly corrupts bytes in the
 * read buffer based on a configurable corruption rate. The actual corruption is
 * applied in {@link net.unit8.rodriguez.fuse.FaultInjectionFS} after reading the real data.
 */
public class CorruptedRead implements FuseFault {
    private double corruptionRate = 0.1;
    private final Random random = new Random();

    /**
     * Constructs a new {@code CorruptedRead} fault with the default corruption rate of 0.1.
     */
    public CorruptedRead() {
    }

    /**
     * Constructs a new {@code CorruptedRead} fault with the specified corruption rate.
     *
     * @param corruptionRate the probability of corruption occurring, between 0.0 and 1.0
     */
    public CorruptedRead(double corruptionRate) {
        this.corruptionRate = corruptionRate;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the normal result unchanged. Actual corruption is applied separately
     * via {@link #corruptBuffer(byte[], int)}.
     *
     * @param normalResult the number of bytes read
     * @return the normal result unchanged
     */
    @Override
    public int apply(int normalResult) {
        // normalResult is the number of bytes read; corruption is applied in FaultInjectionFS.read()
        return normalResult;
    }

    /**
     * Determines whether corruption should be applied based on the corruption rate.
     *
     * @return {@code true} if the random check falls below the corruption rate
     */
    public boolean shouldCorrupt() {
        return random.nextDouble() < corruptionRate;
    }

    /**
     * Corrupts random bytes in the given buffer.
     *
     * <p>The number of bytes corrupted is proportional to the corruption rate.
     *
     * @param buf    the byte buffer to corrupt
     * @param length the number of valid bytes in the buffer
     */
    public void corruptBuffer(byte[] buf, int length) {
        int bytesToCorrupt = Math.max(1, (int) (length * corruptionRate));
        for (int i = 0; i < bytesToCorrupt; i++) {
            int pos = random.nextInt(length);
            buf[pos] = (byte) random.nextInt(256);
        }
    }

    /**
     * Returns the corruption rate.
     *
     * @return the corruption rate, between 0.0 and 1.0
     */
    public double getCorruptionRate() {
        return corruptionRate;
    }

    /**
     * Sets the corruption rate.
     *
     * @param corruptionRate the corruption rate to set, between 0.0 and 1.0
     */
    public void setCorruptionRate(double corruptionRate) {
        this.corruptionRate = corruptionRate;
    }
}
