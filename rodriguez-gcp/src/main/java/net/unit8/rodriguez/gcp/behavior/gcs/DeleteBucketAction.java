package net.unit8.rodriguez.gcp.behavior.gcs;

import net.unit8.rodriguez.gcp.GCSRequest;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Handles {@code DELETE /storage/v1/b/{bucket}} to delete a GCS bucket.
 *
 * <p>Recursively deletes the bucket directory and all objects within it.
 */
public class DeleteBucketAction extends GCSActionBase<Void> {
    /** Creates a new DeleteBucketAction. */
    public DeleteBucketAction() {
    }

    @Override
    public Void handle(GCSRequest request) {
        String bucketName = request.getQueryParam("_bucketName");
        Optional.ofNullable(getGcsDirectory())
                .map(dir -> new File(dir, bucketName).toPath())
                .filter(Files::exists)
                .ifPresent(path -> {
                    try (Stream<Path> walk = Files.walk(path)) {
                        walk.sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        return null;
    }
}
