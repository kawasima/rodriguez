package net.unit8.rodriguez.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parser for Rodriguez harness configuration files.
 *
 * <p>Deserializes JSON configuration into {@link HarnessConfig} using Jackson.
 */
public class ConfigParser {
    final ObjectMapper mapper;

    /**
     * Creates a new configuration parser with a default {@link ObjectMapper}.
     */
    public ConfigParser() {
        this.mapper = new ObjectMapper();
    }

    /**
     * Parses a configuration file into a {@link HarnessConfig}.
     *
     * @param file the JSON configuration file
     * @return the parsed configuration
     * @throws IOException if the file cannot be read or parsed
     */
    public HarnessConfig parse(File file) throws IOException {
        return mapper.readerFor(HarnessConfig.class).readValue(file);
    }

    /**
     * Parses a configuration from an input stream into a {@link HarnessConfig}.
     *
     * @param is the input stream containing JSON configuration
     * @return the parsed configuration
     * @throws IOException if the stream cannot be read or parsed
     */
    public HarnessConfig parse(InputStream is) throws IOException {
        return mapper.readerFor(HarnessConfig.class).readValue(is);
    }

}
