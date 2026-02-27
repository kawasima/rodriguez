package net.unit8.rodriguez.fuse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.unit8.rodriguez.HarnessExtension;

/**
 * A Rodriguez harness extension that provides FUSE filesystem fault injection.
 *
 * <p>Registered via {@code ServiceLoader} and configured under the {@code "fuse"} key
 * in the Rodriguez configuration JSON. Manages the lifecycle of a {@link FuseHarness}.
 */
public class FuseExtension implements HarnessExtension {
    private FuseHarness harness;

    /**
     * Constructs a new {@code FuseExtension}.
     */
    public FuseExtension() {
    }

    @Override
    public String getName() {
        return "fuse";
    }

    @Override
    public void start(JsonNode config) {
        ObjectMapper mapper = new ObjectMapper();
        FuseConfig fuseConfig = mapper.convertValue(config, FuseConfig.class);
        harness = new FuseHarness(fuseConfig);
        harness.start();
    }

    @Override
    public void shutdown() {
        if (harness != null) {
            harness.shutdown();
        }
    }
}
