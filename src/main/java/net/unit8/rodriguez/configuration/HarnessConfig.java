package net.unit8.rodriguez.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.unit8.rodriguez.InstabilityStrategy;

import java.io.Serializable;
import java.util.Map;

public class HarnessConfig implements Serializable {
    private Map<Integer, InstabilityStrategy<?>> ports;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HarnessConfig(@JsonProperty("ports") Map<Integer, InstabilityStrategy<?>> ports) {
        this.ports = ports;
    }

    public Map<Integer, InstabilityStrategy<?>> getPorts() {
        return ports;
    }
}
