package net.unit8.rodriguez.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ConfigParser {
    ObjectMapper mapper;

    public ConfigParser() {
        this.mapper = new ObjectMapper();
    }

    public HarnessConfig parse(File file) throws IOException {
        return mapper.readerFor(HarnessConfig.class).readValue(file);
    }

    public HarnessConfig parse(InputStream is) throws IOException {
        return mapper.readerFor(HarnessConfig.class).readValue(is);
    }

}
