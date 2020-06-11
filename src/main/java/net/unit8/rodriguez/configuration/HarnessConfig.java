package net.unit8.rodriguez.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.unit8.rodriguez.InstabilityStrategy;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class HarnessConfig implements Serializable {
    private final Map<Integer, InstabilityStrategy<?>> ports;
    private final Integer controlPort;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HarnessConfig(
            @JsonProperty("controlPort") Integer controlPort,
            @JsonProperty("ports") Map<Integer, InstabilityStrategy<?>> ports) {
        this.controlPort = controlPort;
        this.ports = ports;
    }

    public Map<Integer, InstabilityStrategy<?>> getPorts() {
        return ports;
    }

    public Optional<Integer> getControlPort() {
        return Optional.ofNullable(controlPort);
    }
}
