package net.unit8.rodriguez;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * SPI interface for Rodriguez harness extensions.
 *
 * <p>Extensions are discovered via {@link java.util.ServiceLoader} and configured
 * under the {@code "extensions"} key in the harness configuration JSON.
 */
public interface HarnessExtension {
    /**
     * Returns the unique name of this extension, used as the key in configuration.
     *
     * @return the extension name
     */
    String getName();

    /**
     * Starts this extension with the given configuration.
     *
     * @param config the extension-specific configuration node
     */
    void start(JsonNode config);

    /**
     * Shuts down this extension and releases any resources.
     */
    void shutdown();
}
