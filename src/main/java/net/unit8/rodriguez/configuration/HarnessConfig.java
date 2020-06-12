package net.unit8.rodriguez.configuration;

import net.unit8.rodriguez.InstabilityStrategy;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class HarnessConfig implements Serializable {
    private Map<Integer, InstabilityStrategy> ports;
    private Integer controlPort;

    public Map<Integer, InstabilityStrategy> getPorts() {
        return ports;
    }

    public HarnessConfig() {

    }

    public void setPorts(Map<Integer, InstabilityStrategy> ports) {
        this.ports = ports;
    }

    public Optional<Integer> getControlPort() {
        return Optional.ofNullable(controlPort);
    }

    public void setControlPort(Integer controlPort) {
        this.controlPort = controlPort;
    }
}
