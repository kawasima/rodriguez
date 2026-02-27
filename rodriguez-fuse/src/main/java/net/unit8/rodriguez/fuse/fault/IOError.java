package net.unit8.rodriguez.fuse.fault;

import jnr.constants.platform.Errno;

/**
 * A FUSE fault that simulates an I/O error.
 *
 * <p>Returns {@code -EIO} for any operation, indicating a generic input/output error.
 */
public class IOError implements FuseFault {
    /**
     * Constructs a new {@code IOError} fault.
     */
    public IOError() {
    }

    /**
     * {@inheritDoc}
     *
     * @param normalResult the normal result value (ignored)
     * @return {@code -EIO} errno value
     */
    @Override
    public int apply(int normalResult) {
        return -Errno.EIO.intValue();
    }
}
