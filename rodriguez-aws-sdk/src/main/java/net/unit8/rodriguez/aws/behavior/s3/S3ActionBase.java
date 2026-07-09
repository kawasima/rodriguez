package net.unit8.rodriguez.aws.behavior.s3;

import net.unit8.rodriguez.aws.MockAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base class for S3 mock actions providing shared access to the S3 storage directory.
 *
 * @param <T> the response type produced by this action
 */
public abstract class S3ActionBase<T> implements MockAction<T> {
    private File s3Directory;

    /**
     * Constructs an S3ActionBase with no directory set.
     */
    public S3ActionBase() {
    }

    /**
     * Returns the list of bucket directories under the S3 storage directory.
     *
     * @return the list of bucket directories, or an empty list if the directory is not set
     */
    public List<File> getBucketDirectories() {
        if (s3Directory == null) {
            return Collections.emptyList();
        }
        return Optional.ofNullable(s3Directory.listFiles(File::isDirectory))
                .map(Arrays::asList)
                .orElse(Collections.emptyList());
    }

    /**
     * Returns the S3 storage directory.
     *
     * @return the S3 directory, or {@code null} if not set
     */
    public File getS3Directory() {
        return s3Directory;
    }

    /**
     * Sets the filesystem directory used for S3 storage.
     *
     * @param directory the directory to use for S3 storage
     */
    public void setS3Directory(File directory) {
        s3Directory = directory;
    }

    /**
     * Safely resolves a bucket directory under the S3 storage root.
     *
     * @param bucketName the bucket name from the request
     * @return the resolved bucket path, guaranteed to be contained within the storage root
     * @throws S3AccessDeniedException if the storage root is unset, the name is
     *                                 invalid, or the resolved path escapes the root
     */
    protected Path resolveBucketPath(String bucketName) {
        return resolveWithin(bucketName);
    }

    /**
     * Safely resolves an object file under the given bucket within the S3 storage root.
     *
     * @param bucketName the bucket name from the request
     * @param objectName the object name from the request
     * @return the resolved object path, guaranteed to be contained within the storage root
     * @throws S3AccessDeniedException if the storage root is unset, a name is
     *                                 invalid, or the resolved path escapes the root
     */
    protected Path resolveObjectPath(String bucketName, String objectName) {
        return resolveWithin(bucketName, objectName);
    }

    /**
     * Resolves the given path segments under the normalized storage root and
     * verifies that the result stays inside it, rejecting {@code ..} / absolute
     * escapes so that no request can read, write, or delete files outside the root.
     */
    private Path resolveWithin(String... segments) {
        if (s3Directory == null) {
            throw new S3AccessDeniedException("S3 storage directory is not configured");
        }
        Path root = s3Directory.toPath().toAbsolutePath().normalize();
        Path resolved = root;
        for (String segment : segments) {
            if (segment == null || segment.isEmpty() || segment.equals("..")) {
                throw new S3AccessDeniedException("Invalid path segment: " + segment);
            }
            resolved = resolved.resolve(segment);
        }
        resolved = resolved.normalize();
        if (!resolved.startsWith(root)) {
            throw new S3AccessDeniedException("Resolved path escapes the storage root: " + resolved);
        }
        verifyRealPathWithin(root, resolved);
        return resolved;
    }

    /**
     * Best-effort symlink defense on top of the lexical containment check above.
     * The lexical check cannot detect a symlink inside the storage root that points
     * outside it, so this resolves the real path of the target (or, for a not-yet-created
     * object, its nearest existing ancestor) and verifies it is still contained within
     * the storage root's real path. Not-yet-existing keys keep resolving normally because
     * only the existing prefix is checked.
     */
    private void verifyRealPathWithin(Path root, Path resolved) {
        try {
            Path existing = resolved;
            while (existing != null && !Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
                existing = existing.getParent();
            }
            if (existing == null) {
                return;
            }
            if (!existing.toRealPath().startsWith(root.toRealPath())) {
                throw new S3AccessDeniedException(
                        "Resolved path escapes the storage root via symlink: " + resolved);
            }
        } catch (IOException e) {
            throw new S3AccessDeniedException("Failed to verify path containment: " + resolved);
        }
    }
}
