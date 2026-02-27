package net.unit8.rodriguez.fuse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.unit8.rodriguez.HarnessExtension;

public class FuseExtension implements HarnessExtension {
    private FuseHarness harness;

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
