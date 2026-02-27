package net.unit8.rodriguez.fuse;

/**
 * Enumeration of FUSE filesystem operations that can be targeted by fault injection rules.
 */
public enum FuseOperation {
    /** File read operation. */
    READ,
    /** File write operation. */
    WRITE,
    /** File open operation. */
    OPEN,
    /** File creation operation. */
    CREATE,
    /** File truncation operation. */
    TRUNCATE,
    /** File synchronization operation. */
    FSYNC,
    /** File flush operation. */
    FLUSH,
    /** Directory creation operation. */
    MKDIR,
    /** File removal (unlink) operation. */
    UNLINK,
    /** Directory removal operation. */
    RMDIR,
    /** File or directory rename operation. */
    RENAME,
    /** File permission change operation. */
    CHMOD,
    /** File ownership change operation. */
    CHOWN
}
