package net.unit8.rodriguez.fuse.fault;

import jnr.constants.platform.Errno;

public class FileNotFound implements FuseFault {
    @Override
    public int apply(int normalResult) {
        return -Errno.ENOENT.intValue();
    }
}
