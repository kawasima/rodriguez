package net.unit8.rodriguez.fuse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FuseHarness {
    private static final Logger LOG = Logger.getLogger(FuseHarness.class.getName());

    private final FuseConfig config;
    private FaultInjectionFS filesystem;
    private Thread fuseThread;

    public FuseHarness(FuseConfig config) {
        this.config = config;
    }

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

    public FuseConfig getConfig() {
        return config;
    }
}
