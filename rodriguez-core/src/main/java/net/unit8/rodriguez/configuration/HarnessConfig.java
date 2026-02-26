package net.unit8.rodriguez.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import net.unit8.rodriguez.InstabilityBehavior;

import java.io.Serializable;
import java.util.*;

public class HarnessConfig implements Serializable {
    private Map<Integer, InstabilityBehavior> ports;
    private Integer controlPort;
    private JsonNode fuse;

    public Map<Integer, InstabilityBehavior> getPorts() {
        return Objects.requireNonNullElse(ports, Collections.emptyMap());
    }

    public void setPorts(Map<Integer, InstabilityBehavior> ports) {
        this.ports = ports;
    }

    public Optional<Integer> getControlPort() {
        return Optional.ofNullable(controlPort);
    }

    public void setControlPort(Integer controlPort) {
        this.controlPort = controlPort;
    }

    public JsonNode getFuse() {
        return fuse;
    }

    public void setFuse(JsonNode fuse) {
        this.fuse = fuse;
    }

    public void merge(HarnessConfig config) {
        config.getControlPort().ifPresent(this::setControlPort);
        if (ports == null) {
            ports = new HashMap<>();
        }
        ports.putAll(config.getPorts());
        if (config.getFuse() != null) {
            this.fuse = config.getFuse();
        }
    }
}
