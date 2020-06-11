package net.unit8.rodriguez;

import net.unit8.rodriguez.configuration.ConfigParser;
import net.unit8.rodriguez.configuration.HarnessConfig;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static picocli.CommandLine.*;

@Command(name = "rodriguez", version = "0.1.0-SNAPSHOT")
public class HarnessServerCommand implements Callable<Integer>, IExitCodeExceptionMapper {
    @Option(names = {"-c", "--config"})
    private File configFile;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Override
    public Integer call() throws Exception {
        HarnessServer server;
        ConfigParser parser = new ConfigParser();
        if (configFile != null && configFile.exists()) {
            HarnessConfig config = parser.parse(configFile);
            server = new HarnessServer(config);
        } else {
            server = new HarnessServer();
        }
        ExecutorService executor = Executors.newCachedThreadPool();
        server.start(executor);
        executor.awaitTermination(10, TimeUnit.MINUTES);
        return 0;
    }

    @Override
    public int getExitCode(Throwable t) {
        t.printStackTrace();
        return 1;
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new HarnessServerCommand());
        commandLine.parseArgs(args);
        if (commandLine.isUsageHelpRequested()) {
            commandLine.usage(System.out);
            return;
        } else if (commandLine.isVersionHelpRequested()) {
            commandLine.printVersionHelp(System.out);
            return;
        }
        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }
}
