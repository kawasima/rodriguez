package net.unit8.rodriguez.fuse.fault;

import jnr.constants.platform.Errno;

/**
 * A FUSE fault that simulates a permission denied condition.
 *
 * <p>Returns {@code -EACCES} for any operation, indicating that the caller lacks
 * the required permissions.
 */
public class PermissionDenied implements FuseFault {
    /**
     * Constructs a new {@code PermissionDenied} fault.
     */
    public PermissionDenied() {
    }

    /**
     * {@inheritDoc}
     *
     * @param normalResult the normal result value (ignored)
     * @return {@code -EACCES} errno value
     */
    @Override
    public int apply(int normalResult) {
        return -Errno.EACCES.intValue();
    }
}
