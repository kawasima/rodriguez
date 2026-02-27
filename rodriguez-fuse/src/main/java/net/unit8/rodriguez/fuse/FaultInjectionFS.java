package net.unit8.rodriguez.fuse;

import jnr.ffi.Pointer;
import jnr.ffi.types.mode_t;
import jnr.ffi.types.off_t;
import jnr.ffi.types.size_t;
import jnr.posix.FileStat;
import net.unit8.rodriguez.fuse.fault.CorruptedRead;
import net.unit8.rodriguez.fuse.fault.FuseFault;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.FuseStubFS;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import ru.serce.jnrfuse.struct.Statvfs;
import ru.serce.jnrfuse.struct.Timespec;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FaultInjectionFS extends FuseStubFS {
    private static final Logger LOG = Logger.getLogger(FaultInjectionFS.class.getName());

    private final Path backingPath;
    private final List<FaultRule> faultRules;

    public FaultInjectionFS(Path backingPath, List<FaultRule> faultRules) {
        this.backingPath = backingPath;
        this.faultRules = faultRules;
    }

    private Path resolveRealPath(String path) {
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        if (relativePath.isEmpty()) {
            return backingPath;
        }
        return backingPath.resolve(relativePath);
    }

    private FuseFault findFault(String path, FuseOperation operation) {
        for (FaultRule rule : faultRules) {
            if (rule.matches(path, operation)) {
                return rule.getFault();
            }
        }
        return null;
    }

    @Override
    public int getattr(String path, ru.serce.jnrfuse.struct.FileStat stat) {
        Path realPath = resolveRealPath(path);
        try {
            if (!Files.exists(realPath)) {
                return -ErrorCodes.ENOENT();
            }
            BasicFileAttributes attrs = Files.readAttributes(realPath, BasicFileAttributes.class);
            if (attrs.isDirectory()) {
                stat.st_mode.set(FileStat.S_IFDIR | 0755);
                stat.st_nlink.set(2);
            } else if (attrs.isRegularFile()) {
                stat.st_mode.set(FileStat.S_IFREG | 0644);
                stat.st_nlink.set(1);
                stat.st_size.set(attrs.size());
            } else if (attrs.isSymbolicLink()) {
                stat.st_mode.set(FileStat.S_IFLNK | 0777);
                stat.st_nlink.set(1);
            }

            try {
                PosixFileAttributes posixAttrs = Files.readAttributes(realPath, PosixFileAttributes.class);
                stat.st_mode.set(toStatMode(posixAttrs));
            } catch (UnsupportedOperationException ignored) {
                // Non-POSIX filesystem
            }

            stat.st_uid.set(getContext().uid.get());
            stat.st_gid.set(getContext().gid.get());

            long mtime = attrs.lastModifiedTime().toMillis() / 1000;
            long atime = attrs.lastAccessTime().toMillis() / 1000;
            stat.st_mtim.tv_sec.set(mtime);
            stat.st_atim.tv_sec.set(atime);

            return 0;
        } catch (IOException e) {
            LOG.log(Level.FINE, "getattr failed for " + path, e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filter, @off_t long offset, FuseFileInfo fi) {
        Path realPath = resolveRealPath(path);
        if (!Files.isDirectory(realPath)) {
            return -ErrorCodes.ENOTDIR();
        }

        filter.apply(buf, ".", null, 0);
        filter.apply(buf, "..", null, 0);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(realPath)) {
            for (Path entry : stream) {
                filter.apply(buf, entry.getFileName().toString(), null, 0);
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "readdir failed for " + path, e);
            return -ErrorCodes.EIO();
        }
        return 0;
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
        FuseFault fault = findFault(path, FuseOperation.OPEN);
        if (fault != null) {
            return fault.apply(0);
        }

        Path realPath = resolveRealPath(path);
        if (!Files.exists(realPath)) {
            return -ErrorCodes.ENOENT();
        }
        return 0;
    }

    @Override
    public int create(String path, @mode_t long mode, FuseFileInfo fi) {
        FuseFault fault = findFault(path, FuseOperation.CREATE);
        if (fault != null) {
            return fault.apply(0);
        }

        Path realPath = resolveRealPath(path);
        try {
            Files.createFile(realPath);
            return 0;
        } catch (IOException e) {
            LOG.log(Level.FINE, "create failed for " + path, e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int read(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        FuseFault fault = findFault(path, FuseOperation.READ);
        if (fault != null && !(fault instanceof CorruptedRead)) {
            return fault.apply(0);
        }

        Path realPath = resolveRealPath(path);
        try {
            byte[] data = Files.readAllBytes(realPath);
            if (offset >= data.length) {
                return 0;
            }
            int bytesToRead = (int) Math.min(size, data.length - offset);
            byte[] readData = new byte[bytesToRead];
            System.arraycopy(data, (int) offset, readData, 0, bytesToRead);

            if (fault instanceof CorruptedRead corruptedRead && corruptedRead.shouldCorrupt()) {
                corruptedRead.corruptBuffer(readData, bytesToRead);
            }

            buf.put(0, readData, 0, bytesToRead);
            return bytesToRead;
        } catch (IOException e) {
            LOG.log(Level.FINE, "read failed for " + path, e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int write(String path, Pointer buf, @size_t long size, @off_t long offset, FuseFileInfo fi) {
        FuseFault fault = findFault(path, FuseOperation.WRITE);
        if (fault != null) {
            int result = fault.apply((int) size);
            if (result < 0) {
                return result;
            }
            // For PartialWrite, write only partial data
            size = result;
        }

        Path realPath = resolveRealPath(path);
        try {
            byte[] data = new byte[(int) size];
            buf.get(0, data, 0, (int) size);

            if (offset == 0 && !Files.exists(realPath)) {
                Files.write(realPath, data);
            } else {
                byte[] existing = Files.exists(realPath) ? Files.readAllBytes(realPath) : new byte[0];
                int newLen = Math.max(existing.length, (int) offset + (int) size);
                byte[] newData = new byte[newLen];
                System.arraycopy(existing, 0, newData, 0, existing.length);
                System.arraycopy(data, 0, newData, (int) offset, (int) size);
                Files.write(realPath, newData);
            }
            return (int) size;
        } catch (IOException e) {
            LOG.log(Level.FINE, "write failed for " + path, e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int truncate(String path, @off_t long size) {
        FuseFault fault = findFault(path, FuseOperation.TRUNCATE);
        if (fault != null) {
            return fault.apply(0);
        }

        Path realPath = resolveRealPath(path);
        try {
            if (size == 0) {
                Files.write(realPath, new byte[0]);
            } else {
                byte[] data = Files.readAllBytes(realPath);
                byte[] truncated = new byte[(int) size];
                System.arraycopy(data, 0, truncated, 0, (int) Math.min(data.length, size));
                Files.write(realPath, truncated);
            }
            return 0;
        } catch (IOException e) {
            LOG.log(Level.FINE, "truncate failed for " + path, e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int fsync(String path, int isdatasync, FuseFileInfo fi) {
        FuseFault fault = findFault(path, FuseOperation.FSYNC);
        if (fault != null) {
            return fault.apply(0);
        }
        return 0;
    }

    @Override
    public int flush(String path, FuseFileInfo fi) {
        FuseFault fault = findFault(path, FuseOperation.FLUSH);
        if (fault != null) {
            return fault.apply(0);
        }
        return 0;
    }

    @Override
    public int mkdir(String path, @mode_t long mode) {
        FuseFault fault = findFault(path, FuseOperation.MKDIR);
        if (fault != null) {
            return fault.apply(0);
        }

        Path realPath = resolveRealPath(path);
        try {
            Files.createDirectory(realPath);
            return 0;
        } catch (IOException e) {
            LOG.log(Level.FINE, "mkdir failed for " + path, e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int unlink(String path) {
        FuseFault fault = findFault(path, FuseOperation.UNLINK);
        if (fault != null) {
            return fault.apply(0);
        }

        Path realPath = resolveRealPath(path);
        try {
            Files.delete(realPath);
            return 0;
        } catch (IOException e) {
            LOG.log(Level.FINE, "unlink failed for " + path, e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int rmdir(String path) {
        FuseFault fault = findFault(path, FuseOperation.RMDIR);
        if (fault != null) {
            return fault.apply(0);
        }

        Path realPath = resolveRealPath(path);
        try {
            Files.delete(realPath);
            return 0;
        } catch (IOException e) {
            LOG.log(Level.FINE, "rmdir failed for " + path, e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int rename(String oldpath, String newpath) {
        FuseFault fault = findFault(oldpath, FuseOperation.RENAME);
        if (fault != null) {
            return fault.apply(0);
        }

        Path realOld = resolveRealPath(oldpath);
        Path realNew = resolveRealPath(newpath);
        try {
            Files.move(realOld, realNew, StandardCopyOption.REPLACE_EXISTING);
            return 0;
        } catch (IOException e) {
            LOG.log(Level.FINE, "rename failed for " + oldpath, e);
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int chmod(String path, @mode_t long mode) {
        FuseFault fault = findFault(path, FuseOperation.CHMOD);
        if (fault != null) {
            return fault.apply(0);
        }
        return 0;
    }

    @Override
    public int chown(String path, long uid, long gid) {
        FuseFault fault = findFault(path, FuseOperation.CHOWN);
        if (fault != null) {
            return fault.apply(0);
        }
        return 0;
    }

    @Override
    public int utimens(String path, Timespec[] timespec) {
        return 0;
    }

    @Override
    public int statfs(String path, Statvfs stbuf) {
        stbuf.f_bsize.set(4096);
        stbuf.f_frsize.set(4096);
        stbuf.f_blocks.set(1024L * 1024);
        stbuf.f_bfree.set(512L * 1024);
        stbuf.f_bavail.set(512L * 1024);
        stbuf.f_files.set(1024L * 1024);
        stbuf.f_ffree.set(512L * 1024);
        stbuf.f_namemax.set(255);
        return 0;
    }

    private int toStatMode(PosixFileAttributes attrs) {
        int mode = 0;
        if (attrs.isDirectory()) {
            mode |= FileStat.S_IFDIR;
        } else if (attrs.isRegularFile()) {
            mode |= FileStat.S_IFREG;
        } else if (attrs.isSymbolicLink()) {
            mode |= FileStat.S_IFLNK;
        }

        Set<PosixFilePermission> perms = attrs.permissions();
        if (perms.contains(PosixFilePermission.OWNER_READ)) mode |= 0400;
        if (perms.contains(PosixFilePermission.OWNER_WRITE)) mode |= 0200;
        if (perms.contains(PosixFilePermission.OWNER_EXECUTE)) mode |= 0100;
        if (perms.contains(PosixFilePermission.GROUP_READ)) mode |= 0040;
        if (perms.contains(PosixFilePermission.GROUP_WRITE)) mode |= 0020;
        if (perms.contains(PosixFilePermission.GROUP_EXECUTE)) mode |= 0010;
        if (perms.contains(PosixFilePermission.OTHERS_READ)) mode |= 0004;
        if (perms.contains(PosixFilePermission.OTHERS_WRITE)) mode |= 0002;
        if (perms.contains(PosixFilePermission.OTHERS_EXECUTE)) mode |= 0001;

        return mode;
    }
}
