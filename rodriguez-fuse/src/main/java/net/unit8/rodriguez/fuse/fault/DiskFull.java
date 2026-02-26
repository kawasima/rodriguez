package net.unit8.rodriguez.fuse.fault;

import jnr.constants.platform.Errno;

public class DiskFull implements FuseFault {
    @Override
    public int apply(int normalResult) {
        return -Errno.ENOSPC.intValue();
    }
}
