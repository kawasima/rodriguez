package net.unit8.rodriguez.fuse.fault;

import jnr.constants.platform.Errno;

/**
 * A FUSE fault that simulates a too many open files condition.
 *
 * <p>Returns {@code -EMFILE} for any operation, indicating that the process has exceeded
 * the maximum number of open file descriptors.
 */
public class TooManyOpenFiles implements FuseFault {
    /**
     * Constructs a new {@code TooManyOpenFiles} fault.
     */
    public TooManyOpenFiles() {
    }

    /**
     * {@inheritDoc}
     *
     * @param normalResult the normal result value (ignored)
     * @return {@code -EMFILE} errno value
     */
    @Override
    public int apply(int normalResult) {
        return -Errno.EMFILE.intValue();
    }
}
