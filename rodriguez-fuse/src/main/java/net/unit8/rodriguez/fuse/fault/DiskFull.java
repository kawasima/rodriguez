package net.unit8.rodriguez.fuse.fault;

import jnr.constants.platform.Errno;

/**
 * A FUSE fault that simulates a disk full condition.
 *
 * <p>Returns {@code -ENOSPC} for any operation, indicating that no space is left on the device.
 */
public class DiskFull implements FuseFault {
    /**
     * Constructs a new {@code DiskFull} fault.
     */
    public DiskFull() {
    }

    /**
     * {@inheritDoc}
     *
     * @param normalResult the normal result value (ignored)
     * @return {@code -ENOSPC} errno value
     */
    @Override
    public int apply(int normalResult) {
        return -Errno.ENOSPC.intValue();
    }
}
