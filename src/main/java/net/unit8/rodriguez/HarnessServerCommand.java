package net.unit8.rodriguez;

import net.unit8.rodriguez.configuration.ConfigParser;
import net.unit8.rodriguez.configuration.HarnessConfig;
import picocli.CommandLine;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.*;

import static picocli.CommandLine.*;

@Command(name = "rodriguez")
public class HarnessServerCommand implements Callable<Integer>, IExitCodeExceptionMapper {
    @Option(names = {"-c", "--config"})
    private File configFile;

    @Override
    public Integer call() throws Exception {
        HarnessConfig config;
        ConfigParser parser = new ConfigParser();
        if (configFile != null && configFile.exists()) {
            config = parser.parse(configFile);
        } else {
            try (InputStream is = HarnessConfig.class.getResourceAsStream("/META-INF/rodriguez/default-config.json")) {
                config = parser.parse(is);
            }
        }
        ExecutorService executor = Executors.newCachedThreadPool();
        new HarnessServer(config).start(executor);
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
