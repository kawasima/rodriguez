package net.unit8.rodriguez.fuse.fault;

import jnr.constants.platform.Errno;

/**
 * A FUSE fault that simulates a read-only filesystem.
 *
 * <p>Returns {@code -EROFS} for any operation, indicating that the filesystem is mounted read-only.
 */
public class ReadOnlyFS implements FuseFault {
    /**
     * Constructs a new {@code ReadOnlyFS} fault.
     */
    public ReadOnlyFS() {
    }

    /**
     * {@inheritDoc}
     *
     * @param normalResult the normal result value (ignored)
     * @return {@code -EROFS} errno value
     */
    @Override
    public int apply(int normalResult) {
        return -Errno.EROFS.intValue();
    }
}
