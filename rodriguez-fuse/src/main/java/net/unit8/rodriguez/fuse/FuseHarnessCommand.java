package net.unit8.rodriguez.fuse;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "rodriguez-fuse", version = "0.3.0-SNAPSHOT")
public class FuseHarnessCommand implements Callable<Integer> {
    @Option(names = {"-c", "--config"}, required = true, description = "FUSE config file")
    private File configFile;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Override
    public Integer call() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        FuseConfig config = mapper.readValue(configFile, FuseConfig.class);

        FuseHarness harness = new FuseHarness(config);
        harness.start();

        Runtime.getRuntime().addShutdownHook(new Thread(harness::shutdown));

        Thread.currentThread().join();
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new FuseHarnessCommand()).execute(args);
        System.exit(exitCode);
    }
}
