package net.unit8.rodriguez;

import net.unit8.rodriguez.configuration.ConfigParser;
import net.unit8.rodriguez.configuration.HarnessConfig;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;

import static picocli.CommandLine.*;

/**
 * CLI entry point for the Rodriguez harness server.
 *
 * <p>Uses PicoCLI to parse command-line options such as {@code --config} for specifying
 * a configuration file. Starts the {@link HarnessServer} and blocks until shutdown.
 */
@Command(name = "rodriguez", version = "0.1.0")
public class HarnessServerCommand implements Callable<Integer>, IExitCodeExceptionMapper {
    static {
        try (InputStream logProps = Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties")){
            if (logProps != null) {
                LogManager.getLogManager().readConfiguration(logProps);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Creates a new harness server command instance.
     */
    public HarnessServerCommand() {
    }

    @Option(names = {"-c", "--config"})
    private File configFile;

    @Option(names = {"-V", "--version"}, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Override
    public Integer call() throws Exception {
        HarnessServer server = new HarnessServer();
        if (configFile != null && configFile.exists()) {
            ConfigParser parser = new ConfigParser();
            HarnessConfig config = parser.parse(configFile);
            server.getConfig().merge(config);
        }
        ExecutorService executor = Executors.newCachedThreadPool();
        server.start(executor);
        server.await();
        return 0;
    }

    @Override
    public int getExitCode(Throwable t) {
        t.printStackTrace();
        return 1;
    }

    /**
     * Application entry point that parses arguments and starts the harness server.
     *
     * @param args the command-line arguments
     */
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
