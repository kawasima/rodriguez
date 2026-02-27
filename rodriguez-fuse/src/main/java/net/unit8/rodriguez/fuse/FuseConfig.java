package net.unit8.rodriguez.fuse;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the FUSE fault injection filesystem.
 *
 * <p>Specifies the mount path, backing storage path, and a list of fault rules
 * that determine which faults to inject for specific file operations.
 */
public class FuseConfig {
    private String mountPath;
    private String backingPath;
    private List<FaultRule> faults = new ArrayList<>();

    /**
     * Constructs a new {@code FuseConfig} with default values.
     */
    public FuseConfig() {
    }

    /**
     * Returns the FUSE mount path.
     *
     * @return the mount path
     */
    public String getMountPath() {
        return mountPath;
    }

    /**
     * Sets the FUSE mount path.
     *
     * @param mountPath the mount path to set
     */
    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    /**
     * Returns the backing storage path.
     *
     * @return the backing path where real files are stored
     */
    public String getBackingPath() {
        return backingPath;
    }

    /**
     * Sets the backing storage path.
     *
     * @param backingPath the backing path to set
     */
    public void setBackingPath(String backingPath) {
        this.backingPath = backingPath;
    }

    /**
     * Returns the list of fault rules.
     *
     * @return the list of fault rules
     */
    public List<FaultRule> getFaults() {
        return faults;
    }

    /**
     * Sets the list of fault rules.
     *
     * @param faults the list of fault rules to set
     */
    public void setFaults(List<FaultRule> faults) {
        this.faults = faults;
    }
}
