package net.unit8.rodriguez.configuration;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import net.unit8.rodriguez.InstabilityBehavior;
import net.unit8.rodriguez.behavior.RefuseConnection;
import net.unit8.rodriguez.behavior.SlowResponse;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HarnessConfigTest {

    @Test
    void mergeOverridesControlPort() {
        HarnessConfig base = new HarnessConfig();
        base.setControlPort(10200);

        HarnessConfig override = new HarnessConfig();
        override.setControlPort(19200);

        base.merge(override);

        assertThat(base.getControlPort()).hasValue(19200);
    }

    @Test
    void mergeKeepsBaseControlPortWhenOverrideHasNone() {
        HarnessConfig base = new HarnessConfig();
        base.setControlPort(10200);

        HarnessConfig override = new HarnessConfig();
        // no controlPort set in override

        base.merge(override);

        assertThat(base.getControlPort()).hasValue(10200);
    }

    @Test
    void mergeAddsPortsAdditively() {
        HarnessConfig base = new HarnessConfig();
        base.setPorts(new HashMap<>(Map.of(10201, new RefuseConnection())));

        HarnessConfig override = new HarnessConfig();
        override.setPorts(Map.of(10205, new SlowResponse()));

        base.merge(override);

        assertThat(base.getPorts()).containsKey(10201);
        assertThat(base.getPorts()).containsKey(10205);
    }

    @Test
    void mergeOverridesExistingPort() {
        HarnessConfig base = new HarnessConfig();
        InstabilityBehavior original = new RefuseConnection();
        base.setPorts(new HashMap<>(Map.of(10201, original)));

        HarnessConfig override = new HarnessConfig();
        InstabilityBehavior replacement = new SlowResponse();
        override.setPorts(Map.of(10201, replacement));

        base.merge(override);

        assertThat(base.getPorts().get(10201)).isInstanceOf(SlowResponse.class);
    }

    @Test
    void mergeAddsExtensions() {
        HarnessConfig base = new HarnessConfig();

        HarnessConfig override = new HarnessConfig();
        override.setExtensions(Map.of(
                "proxy", JsonNodeFactory.instance.objectNode().put("port", 10220)
        ));

        base.merge(override);

        assertThat(base.getExtensions()).containsKey("proxy");
        assertThat(base.getExtensions().get("proxy").get("port").asInt()).isEqualTo(10220);
    }

    @Test
    void mergeEmptyExtensionsLeavesBaseUnchanged() {
        HarnessConfig base = new HarnessConfig();
        base.setExtensions(Map.of(
                "existing", JsonNodeFactory.instance.objectNode()
        ));

        HarnessConfig override = new HarnessConfig();
        // no extensions

        base.merge(override);

        assertThat(base.getExtensions()).containsKey("existing");
    }

    @Test
    void emptyBaseReceivesAllFromOverride() {
        HarnessConfig base = new HarnessConfig();

        HarnessConfig override = new HarnessConfig();
        override.setControlPort(10200);
        override.setPorts(Map.of(10201, new SlowResponse()));

        base.merge(override);

        assertThat(base.getControlPort()).hasValue(10200);
        assertThat(base.getPorts()).containsKey(10201);
    }

    @Test
    void getPortsReturnsEmptyMapWhenUnset() {
        HarnessConfig config = new HarnessConfig();
        assertThat(config.getPorts()).isEmpty();
    }

    @Test
    void getExtensionsReturnsEmptyMapWhenUnset() {
        HarnessConfig config = new HarnessConfig();
        assertThat(config.getExtensions()).isEmpty();
    }

    @Test
    void getControlPortReturnsEmptyWhenUnset() {
        HarnessConfig config = new HarnessConfig();
        assertThat(config.getControlPort()).isEmpty();
    }
}
