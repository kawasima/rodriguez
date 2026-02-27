package net.unit8.rodriguez.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import net.unit8.rodriguez.InstabilityBehavior;

import java.io.Serializable;
import java.util.*;

/**
 * Configuration for the Rodriguez harness server.
 *
 * <p>Defines port-to-behavior mappings, the control server port, and extension
 * configurations. Multiple configurations can be merged together using {@link #merge(HarnessConfig)}.
 */
public class HarnessConfig implements Serializable {
    /** Port-to-behavior mapping. */
    private Map<Integer, InstabilityBehavior> ports;
    /** The control server port number. */
    private Integer controlPort;
    /** Extension name to configuration mapping. */
    private Map<String, JsonNode> extensions;

    /**
     * Creates a new empty harness configuration.
     */
    public HarnessConfig() {
    }

    /**
     * Returns the port-to-behavior mapping.
     *
     * @return an unmodifiable map of port numbers to behaviors, or an empty map if not set
     */
    public Map<Integer, InstabilityBehavior> getPorts() {
        return Objects.requireNonNullElse(ports, Collections.emptyMap());
    }

    /**
     * Sets the port-to-behavior mapping.
     *
     * @param ports a map of port numbers to instability behaviors
     */
    public void setPorts(Map<Integer, InstabilityBehavior> ports) {
        this.ports = ports;
    }

    /**
     * Returns the control server port, if configured.
     *
     * @return an {@link Optional} containing the control port, or empty if not set
     */
    public Optional<Integer> getControlPort() {
        return Optional.ofNullable(controlPort);
    }

    /**
     * Sets the control server port.
     *
     * @param controlPort the port number for the control server
     */
    public void setControlPort(Integer controlPort) {
        this.controlPort = controlPort;
    }

    /**
     * Returns the extension configurations.
     *
     * @return a map of extension names to their JSON configurations, or an empty map if not set
     */
    public Map<String, JsonNode> getExtensions() {
        return Objects.requireNonNullElse(extensions, Collections.emptyMap());
    }

    /**
     * Sets the extension configurations.
     *
     * @param extensions a map of extension names to their JSON configurations
     */
    public void setExtensions(Map<String, JsonNode> extensions) {
        this.extensions = extensions;
    }

    /**
     * Merges another configuration into this one.
     *
     * <p>The given configuration's values override this configuration's values.
     * Port mappings and extensions are merged additively.
     *
     * @param config the configuration to merge into this one
     */
    public void merge(HarnessConfig config) {
        config.getControlPort().ifPresent(this::setControlPort);
        if (ports == null) {
            ports = new HashMap<>();
        }
        ports.putAll(config.getPorts());
        if (!config.getExtensions().isEmpty()) {
            if (extensions == null) {
                extensions = new HashMap<>();
            }
            extensions.putAll(config.getExtensions());
        }
    }
}
