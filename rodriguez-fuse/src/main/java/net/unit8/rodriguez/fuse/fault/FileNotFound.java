package net.unit8.rodriguez.fuse.fault;

import jnr.constants.platform.Errno;

/**
 * A FUSE fault that simulates a file not found condition.
 *
 * <p>Returns {@code -ENOENT} for any operation, indicating that the file or directory does not exist.
 */
public class FileNotFound implements FuseFault {
    /**
     * Constructs a new {@code FileNotFound} fault.
     */
    public FileNotFound() {
    }

    /**
     * {@inheritDoc}
     *
     * @param normalResult the normal result value (ignored)
     * @return {@code -ENOENT} errno value
     */
    @Override
    public int apply(int normalResult) {
        return -Errno.ENOENT.intValue();
    }
}
