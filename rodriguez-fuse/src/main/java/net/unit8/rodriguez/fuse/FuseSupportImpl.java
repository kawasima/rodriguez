package net.unit8.rodriguez.fuse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.unit8.rodriguez.FuseSupport;

public class FuseSupportImpl implements FuseSupport {
    private FuseHarness harness;

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
