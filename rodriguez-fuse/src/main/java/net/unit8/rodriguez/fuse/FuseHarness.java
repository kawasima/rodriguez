package net.unit8.rodriguez.fuse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the lifecycle of a FUSE fault injection filesystem.
 *
 * <p>Creates the mount and backing directories, mounts the {@link FaultInjectionFS},
 * and provides methods to start and shut down the filesystem.
 */
public class FuseHarness {
    private static final Logger LOG = Logger.getLogger(FuseHarness.class.getName());

    private final FuseConfig config;
    private FaultInjectionFS filesystem;
    private Thread fuseThread;

    /**
     * Constructs a new {@code FuseHarness} with the given configuration.
     *
     * @param config the FUSE configuration specifying mount path, backing path, and fault rules
     */
    public FuseHarness(FuseConfig config) {
        this.config = config;
    }

    /**
     * Starts the FUSE filesystem.
     *
     * <p>Creates the mount and backing directories if they do not exist,
     * then mounts the fault injection filesystem in a daemon thread.
     */
    public void start() {
        Path mountPath = Path.of(config.getMountPath());
        Path backingPath = Path.of(config.getBackingPath());

        try {
            Files.createDirectories(mountPath);
            Files.createDirectories(backingPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create directories", e);
        }

        filesystem = new FaultInjectionFS(backingPath, config.getFaults());

        fuseThread = new Thread(() -> {
            try {
                filesystem.mount(mountPath, true, false);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "FUSE mount failed", e);
            }
        }, "rodriguez-fuse");
        fuseThread.setDaemon(true);
        fuseThread.start();

        LOG.info("FUSE filesystem mounted at " + mountPath + " (backing: " + backingPath + ")");
    }

    /**
     * Shuts down the FUSE filesystem by unmounting it and interrupting the FUSE thread.
     */
    public void shutdown() {
        if (filesystem != null) {
            try {
                filesystem.umount();
                LOG.info("FUSE filesystem unmounted");
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to unmount FUSE filesystem", e);
            }
        }
        if (fuseThread != null) {
            fuseThread.interrupt();
        }
    }

    /**
     * Returns the FUSE configuration.
     *
     * @return the FUSE configuration
     */
    public FuseConfig getConfig() {
        return config;
    }
}
