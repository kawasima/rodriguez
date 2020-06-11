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

@Command(name = "rodriguez")
public class HarnessServerCommand implements Callable<Integer>, IExitCodeExceptionMapper {
    @Option(names = {"-c", "--config"})
    private File configFile;

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
    public int getExitCode(Throwable throwable) {
        return 1;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new HarnessServerCommand()).execute(args);
        System.exit(exitCode);
    }
}
