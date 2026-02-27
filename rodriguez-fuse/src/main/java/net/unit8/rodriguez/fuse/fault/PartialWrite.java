package net.unit8.rodriguez.fuse.fault;

/**
 * A FUSE fault that simulates partial write operations.
 *
 * <p>Instead of writing all requested bytes, this fault reduces the number of bytes
 * written according to a configurable write ratio. This simulates scenarios where
 * the filesystem only partially completes a write request.
 */
public class PartialWrite implements FuseFault {
    private double writeRatio = 0.5;

    /**
     * Constructs a new {@code PartialWrite} fault with the default write ratio of 0.5.
     */
    public PartialWrite() {
    }

    /**
     * Constructs a new {@code PartialWrite} fault with the specified write ratio.
     *
     * @param writeRatio the ratio of bytes to actually write, between 0.0 and 1.0
     */
    public PartialWrite(double writeRatio) {
        this.writeRatio = writeRatio;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reduces the number of bytes written by applying the write ratio.
     * Always writes at least 1 byte if the normal result is positive.
     *
     * @param normalResult the number of bytes requested to write
     * @return the reduced number of bytes to write, or the normal result if not positive
     */
    @Override
    public int apply(int normalResult) {
        if (normalResult > 0) {
            return Math.max(1, (int) (normalResult * writeRatio));
        }
        return normalResult;
    }

    /**
     * Returns the write ratio.
     *
     * @return the write ratio, between 0.0 and 1.0
     */
    public double getWriteRatio() {
        return writeRatio;
    }

    /**
     * Sets the write ratio.
     *
     * @param writeRatio the write ratio to set, between 0.0 and 1.0
     */
    public void setWriteRatio(double writeRatio) {
        this.writeRatio = writeRatio;
    }
}
