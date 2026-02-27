package net.unit8.rodriguez.fuse;

import com.fasterxml.jackson.databind.ObjectMapper;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * CLI command for running the FUSE fault injection filesystem as a standalone process.
 *
 * <p>Reads a JSON configuration file and mounts a {@link FaultInjectionFS} that remains
 * running until the process is terminated.
 */
@Command(name = "rodriguez-fuse", version = "0.3.0")
public class FuseHarnessCommand implements Callable<Integer> {
    @Option(names = {"-c", "--config"}, required = true, description = "FUSE config file")
    private File configFile;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    /**
     * Constructs a new {@code FuseHarnessCommand}.
     */
    public FuseHarnessCommand() {
    }

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

    /**
     * Entry point for the FUSE harness command-line application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new FuseHarnessCommand()).execute(args);
        System.exit(exitCode);
    }
}
