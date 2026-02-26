package net.unit8.rodriguez.fuse;

import java.util.ArrayList;
import java.util.List;

public class FuseConfig {
    private String mountPath;
    private String backingPath;
    private List<FaultRule> faults = new ArrayList<>();

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public String getBackingPath() {
        return backingPath;
    }

    public void setBackingPath(String backingPath) {
        this.backingPath = backingPath;
    }

    public List<FaultRule> getFaults() {
        return faults;
    }

    public void setFaults(List<FaultRule> faults) {
        this.faults = faults;
    }
}
