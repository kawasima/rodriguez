package net.unit8.rodriguez.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Resolves untrusted bucket/object names to filesystem paths guaranteed to stay
 * within a storage root, using {@link PathContainment}.
 *
 * <p>This is the single home for the two-step "resolve the bucket, then resolve the
 * object strictly within that bucket, canonicalizing the real root once and failing
 * closed" composition shared by the S3 and GCS mocks. They previously each carried a
 * copy that differed only in the thrown exception type, so a change to the composition
 * had to be mirrored by hand and could silently diverge. Callers supply an
 * {@link ErrorFactory} that maps a resolution failure onto their protocol's exception.
 */
public final class ContainedStorage {

    /**
     * Maps a resolution failure onto a module-specific runtime exception.
     */
    public interface ErrorFactory {
        /**
         * The storage root is missing or its canonical form could not be resolved.
         *
         * @param message a human-readable description
         * @return the exception to throw
         */
        RuntimeException storageUnavailable(String message);

        /**
         * An untrusted name is missing, malformed, or escapes its container.
         *
         * @param kind {@code "bucket"} or {@code "object"}
         * @param name the offending name
         * @return the exception to throw
         */
        RuntimeException invalidName(String kind, String name);
    }

    private final File directory;
    private final ErrorFactory errors;

    /**
     * Creates a resolver rooted at {@code directory}.
     *
     * @param directory the storage root (may be {@code null}; resolution then fails closed)
     * @param errors    maps failures to protocol-specific exceptions
     */
    public ContainedStorage(File directory, ErrorFactory errors) {
        this.directory = directory;
        this.errors = errors;
    }

    /**
     * Resolves a bucket directory under the storage root.
     *
     * @param bucketName the untrusted bucket name
     * @return the contained bucket path
     */
    public Path resolveBucket(String bucketName) {
        Path root = storageRoot();
        return PathContainment.resolveWithin(root, storageRootReal(root), bucketName, false)
                .orElseThrow(() -> errors.invalidName("bucket", bucketName));
    }

    /**
     * Resolves an object under the given bucket, contained within that bucket
     * directory (not merely the storage root), so an object key such as
     * {@code ../other-bucket/key} cannot cross into a sibling bucket.
     *
     * @param bucketName the untrusted bucket name
     * @param objectName the untrusted object name (may contain {@code /} separators)
     * @return the contained object path
     */
    public Path resolveObject(String bucketName, String objectName) {
        Path root = storageRoot();
        Path rootReal = storageRootReal(root);
        Path bucketPath = PathContainment.resolveWithin(root, rootReal, bucketName, false)
                .orElseThrow(() -> errors.invalidName("bucket", bucketName));
        // Bound the object to the bucket's OWN real path, not just the storage root, so a
        // symlink inside the bucket cannot redirect the key into a sibling bucket while
        // still satisfying the root boundary. Fall back to the root boundary when the
        // bucket directory does not exist yet (nothing to redirect through).
        Path objectBoundary = realPathOrElse(bucketPath, rootReal);
        return PathContainment.resolveWithin(bucketPath, objectBoundary, objectName, false)
                .orElseThrow(() -> errors.invalidName("object", objectName));
    }

    private static Path realPathOrElse(Path path, Path fallback) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return fallback;
        }
    }

    private Path storageRoot() {
        if (directory == null) {
            throw errors.storageUnavailable("storage directory is not configured");
        }
        return directory.toPath().toAbsolutePath().normalize();
    }

    private Path storageRootReal(Path root) {
        try {
            return root.toRealPath();
        } catch (IOException e) {
            throw errors.storageUnavailable("failed to resolve storage root: " + e.getMessage());
        }
    }
}
