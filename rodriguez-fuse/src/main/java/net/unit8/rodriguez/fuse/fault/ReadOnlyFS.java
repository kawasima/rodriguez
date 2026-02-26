package net.unit8.rodriguez.fuse.fault;

import jnr.constants.platform.Errno;

public class ReadOnlyFS implements FuseFault {
    @Override
    public int apply(int normalResult) {
        return -Errno.EROFS.intValue();
    }
}
