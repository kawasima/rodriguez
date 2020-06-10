package net.unit8.rodriguez;

import net.unit8.rodriguez.configuration.ConfigParser;
import net.unit8.rodriguez.configuration.HarnessConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class HarnessServerTest {
    @Test
    void readConfig() throws IOException {
        try (InputStream is = HarnessConfig.class.getResourceAsStream("/META-INF/rodriguez/default-config.json")) {
            HarnessConfig config = new ConfigParser().parse(is);
            assertThat(config).isNotNull();
        }
    }
}