package net.unit8.rodriguez.configuration;

import net.unit8.rodriguez.InstabilityBehavior;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class HarnessConfig implements Serializable {
    private Map<Integer, ? extends InstabilityBehavior> ports;
    private Integer controlPort;

    public Map<Integer, ? extends InstabilityBehavior> getPorts() {
        return ports;
    }

    public HarnessConfig() {

    }

    public void setPorts(Map<Integer, ? extends InstabilityBehavior> ports) {
        this.ports = ports;
    }

    public Optional<Integer> getControlPort() {
        return Optional.ofNullable(controlPort);
    }

    public void setControlPort(Integer controlPort) {
        this.controlPort = controlPort;
    }
}
