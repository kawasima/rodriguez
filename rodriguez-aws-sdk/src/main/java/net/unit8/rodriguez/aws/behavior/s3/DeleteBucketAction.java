package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.AWSRequest;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

public class DeleteBucketAction extends S3ActionBase<Void>{
    @Override
    public Void handle(AWSRequest params) {
        Optional.ofNullable(params.getParams().getFirst("BucketName"))
                .ifPresent(bucket -> {
                    try {
                        Files.walk(getS3Directory().toPath().resolve(bucket))
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        return null;
    }
}
